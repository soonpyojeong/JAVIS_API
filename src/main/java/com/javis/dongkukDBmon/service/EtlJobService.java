package com.javis.dongkukDBmon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javis.dongkukDBmon.Dto.*;
import com.javis.dongkukDBmon.config.*;
import com.javis.dongkukDBmon.model.*;
import com.javis.dongkukDBmon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.apache.camel.ProducerTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJobLog;

import javax.sql.DataSource;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.util.stream.Collectors;

import static com.javis.dongkukDBmon.config.AesUtil.decrypt;
import static org.apache.camel.util.StringHelper.toJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlJobService {

    private final ObjectMapper objectMapper;
    private final EtlJobRepository etlJobRepo;
    private final DbConnectionInfoRepository dbRepo;
    private final EtlBatchRepository batchRepo;
    private final EtlJobLogRepository jobLogRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final MonitorModuleRepository monitorModuleRepo;
    @Value("${aes.key}")
    private String aesKey;

    private final ProducerTemplate producerTemplate;
    private final EtlBatchService batchService;
    private  final JdbcTemplate jdbcTemplate;
    private  final EtlJobRetryLogRepository retryLogRepo;

    // 예시: 추출 함수
    public List<Map<String, Object>> extractRows(String sql) {
        // 이 쿼리는 Statement로 실행됨 (Tibero 호환 OK)
        return jdbcTemplate.queryForList(sql);
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!etlJobRepo.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 JOB입니다. ID: " + id);
        }
        etlJobRepo.deleteById(id);
    }


    public void notifyJobStatus(EtlJob job) {
        EtlJobLog lastLog = jobLogRepo.findLatestLogByJobId(job.getId());
        JobStatusMessage msg = lastLog != null
                ? new JobStatusMessage(job.getId(), lastLog.getResult(), lastLog.getExecutedAt())
                : new JobStatusMessage(job.getId(), null, null);

        messagingTemplate.convertAndSend("/topic/etl-job-status", msg);
    }



    // ETL 실행 후 (성공/실패 등 상태변경 직후)
    private void afterEtlRun(EtlJob job) {
        // DB에 상태 저장 후 바로 WebSocket 알림
        notifyJobStatus(job);
    }
    // CRUD
    public Long createJob(EtlJobDto dto) {
        try {
            EtlJob job = new EtlJob();
            job.setJobName(dto.getJobName());
            job.setSourceDbIdsJson(objectMapper.writeValueAsString(dto.getSourceDbIds()));
            job.setMonitorModuleId(dto.getMonitorModuleId());
            job.setTargetDbId(dto.getTargetDbId());
            job.setTargetTable(dto.getTargetTable());
            job.setSchedule(dto.getSchedule());
            job.setStatus(dto.getStatus());

            // ✅ extractQueries → extractQueryJson
            if (dto.getExtractQueries() != null && !dto.getExtractQueries().isEmpty()) {
                String queryJson = objectMapper.writeValueAsString(dto.getExtractQueries());
                job.setExtractQueryJson(queryJson);
            }

            // 기존 extractQuery 설정 로직 (하위 호환용)
            if (dto.getExtractQuery() == null || dto.getExtractQuery().isBlank()) {
                Optional<MonitorModule> optModule = monitorModuleRepo.findById(dto.getMonitorModuleId());
                Optional<DbConnectionInfo> optDb = dbRepo.findById(dto.getSourceDbIds().get(0)); // 임시로 첫 번째만 사용

                if (optModule.isPresent() && optDb.isPresent()) {
                    String dbType = optDb.get().getDbType();

                    String query = optModule.get().getQueries().stream()
                            .filter(q -> q.getDbType().equalsIgnoreCase(dbType))
                            .map(MonitorModuleQuery::getQueryText)
                            .findFirst()
                            .orElse(null);

                    if (query != null) {
                        job.setExtractQuery(query);
                    }
                }
            }

            etlJobRepo.save(job);
            return job.getId();
        } catch (Exception e) {
            log.error("잡 등록 중 오류", e);
            throw new RuntimeException("잡 등록 실패: " + e.getMessage(), e);
        }
    }

    public void retryEtlJob(Long jobId, Long sourceDbId) {
        log.debug("🔍 [retryEtlJob] 시작 - jobId: {}, sourceDbId: {}", jobId, sourceDbId);

        EtlJob job = etlJobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("해당 jobId가 존재하지 않습니다: " + jobId));

        DbConnectionInfo dbInfo = dbRepo.findById(sourceDbId)
                .orElseThrow(() -> new IllegalArgumentException("해당 sourceDbId가 존재하지 않습니다: " + sourceDbId));

        String dbType = dbInfo.getDbType(); // 예: ORACLE, TIBERO
        Map<String, String> queryMap = job.getExtractQueries();

        log.debug("🔎 DB타입: {}, 쿼리맵: {}", dbType, queryMap);

        String query = queryMap.get(dbType); // ← 타입 일치하게 수정!

        if (query == null) {
            throw new IllegalStateException("해당 DB 타입(" + dbType + ")에 대한 쿼리가 존재하지 않습니다.");
        }

        log.debug("📋 [추출 쿼리 확인] {}", query);

        producerTemplate.sendBody("direct:runEtlJob", jobId);
        log.debug("🚀 [Job 실행 요청 완료] Camel 전달 완료");
    }



    public List<EtlJobLogDto> getJobLogDtos(Long jobId) {
        List<EtlJobLog> logs = jobLogRepo.findByBatchId(jobId);
        return logs.stream().map(log -> {
            EtlJobLogDto dto = new EtlJobLogDto();
            dto.setJobId(log.getLogId());                         // ✅ 필수
            dto.setSourceDbId(log.getSourceDbId());               // ✅ 필수
            dto.setSourceDbName(getDbName(log.getSourceDbId()));  // ✅ 보여줄 이름
            dto.setResult(log.getResult());
            dto.setMessage(log.getMessage());
            dto.setExecutedAt(log.getExecutedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    private String getDbName(Long dbId) {
        return dbRepo.findById(dbId)
                .map(DbConnectionInfo::getDbName)
                .orElse("UNKNOWN");
    }

    public void updateJob(Long id, EtlJobDto dto) {
        EtlJob job = etlJobRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("JOB 없음"));

        job.setJobName(dto.getJobName());
        job.setTargetDbId(dto.getTargetDbId());
        job.setTargetTable(dto.getTargetTable());
        job.setSchedule(dto.getSchedule());
        job.setStatus(dto.getStatus());
        job.setMonitorModuleId(dto.getMonitorModuleId());

        // ✅ sourceDbIds → JSON 문자열
        try {
            job.setSourceDbIdsJson(objectMapper.writeValueAsString(dto.getSourceDbIds()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("sourceDbIds 변환 실패", e);
        }

        // ✅ extractQueries → extractQueryJson
        try {
            if (dto.getExtractQueries() != null && !dto.getExtractQueries().isEmpty()) {
                job.setExtractQueryJson(objectMapper.writeValueAsString(dto.getExtractQueries()));
            } else {
                job.setExtractQueryJson(null); // 쿼리 제거할 수도 있음
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("extractQueries 변환 실패", e);
        }

        etlJobRepo.save(job);
    }
    public List<EtlJobLogDto> getAllJobLogDtos(Long jobId) {
        List<EtlBatch> batches = batchRepo.findByJobId(jobId);
        if (batches.isEmpty()) return List.of();

        List<Long> batchIds = batches.stream()
                .map(EtlBatch::getBatchId)
                .collect(Collectors.toList());

        List<EtlJobLog> logs = jobLogRepo.findByBatchIdIn(batchIds);

        Map<Long, String> dbNameMap = dbRepo.findAll().stream()
                .collect(Collectors.toMap(DbConnectionInfo::getId, DbConnectionInfo::getDbName));

        return logs.stream().map(log -> {
            EtlJobLogDto dto = new EtlJobLogDto();
            dto.setExecutedAt(log.getExecutedAt());
            dto.setResult(log.getResult());
            dto.setMessage(log.getMessage());

            // ✅ [여기!] batch → jobId 추출
            EtlBatch batch = batchRepo.findById(log.getBatchId()).orElse(null);
            if (batch != null) {
                dto.setJobId(batch.getJobId()); // ✅ jobId 세팅
            }

            dto.setSourceDbId(log.getSourceDbId());
            dto.setSourceDbName(
                    Optional.ofNullable(log.getSourceDbId())
                            .map(id -> dbNameMap.getOrDefault(id, "알 수 없음"))
                            .orElse("알 수 없음")
            );

            return dto;
        }).toList();
    }





    public EtlJob getJob(Long id) { return etlJobRepo.findById(id).orElseThrow(); }
    public List<EtlJob> listJobs() { return etlJobRepo.findAll(); }
    public List<EtlJobLog> getJobLogs(Long jobId) {
        EtlBatch latest = batchRepo.findLatestBatchByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("최근 배치 없음: jobId=" + jobId));
        return jobLogRepo.findByBatchId(latest.getBatchId());
    }


    public EtlJobLog getLastLog(Long jobId) {
        return jobLogRepo.findLatestLogByJobId(jobId);
    }



    public String runEtlJob(Long jobId) {
        EtlJob job = etlJobRepo.findById(jobId).orElseThrow();

        Long batchId = null;

        try {
            batchId = batchService.startBatch(jobId); // ⬅️ 배치 시작
            producerTemplate.sendBody("direct:runEtlJob", jobId); // 실행

            job.setLastRunAt(new Date());
            job.setLastResult("SUCCESS");
            etlJobRepo.save(job);


            batchService.endBatch(batchId, true, "성공");
            notifyJobStatus(job);
            return "ETL 실행 성공";
        } catch (Exception e) {
            String root = getRootCauseMessage(e);

            job.setLastRunAt(new Date());
            job.setLastResult("FAIL: " + root);
            etlJobRepo.save(job);

            if (batchId == null) batchId = batchService.startBatch(jobId); // 실패지만 로그 위해 강제 배치 생성

            batchService.endBatch(batchId, false, root);

            notifyJobStatus(job);
            return "실패: " + root;
        }
    }






    /** Exception의 root cause message만 반환하는 유틸 */
    private String getRootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }


    /**
     * 암호문일 때만 복호화, 평문이면 그대로 반환 (Base64 패턴 등으로 구분 가능)
     */
    private String tryDecrypt(String aesKey, String value) {
        if (value == null || value.isEmpty()) return "";
        // 예: 길이/패턴 체크, 또는 복호화 실패시 평문 그대로
        try {
            return decrypt(aesKey, value);
        } catch (Exception e) {
            // 복호화 실패시 원본(평문) 반환 (예: 이미 복호화된 경우)
            return value;
        }
    }


    public List<JobListDto> getJobsWithLastLog() {
        List<EtlJob> jobs = etlJobRepo.findAll();

        return jobs.stream().map(job -> {
            EtlBatch latestBatch = batchRepo.findLatestBatchByJobId(job.getId())
                    .orElseThrow(() -> new IllegalStateException("최근 배치 정보를 찾을 수 없습니다. jobId=" + job.getId()));


            String lastResult = null;
            Date lastRunAt = null;

            if (latestBatch != null) {
                Long batchId = latestBatch.getBatchId();

                // 🔹 11g 호환 방식: int → boolean 처리
                boolean hasFail = jobLogRepo.hasFailLogInBatch(batchId) > 0;
                lastResult = hasFail ? "FAIL" : "SUCCESS";

                EtlJobLog latestLog = jobLogRepo.findTop1ByBatchIdOrderByExecutedAtDesc(batchId);
                if (latestLog != null) {
                    lastRunAt = latestLog.getExecutedAt();
                }
            }

            return new JobListDto(job.getId(), job.getJobName(), lastResult, lastRunAt);
        }).toList();
    }


    // 1. 프론트에서 재수행할 로그의 logId를 넘겨줌
    public EtlJobRetryLog retryJob(Long logId, String triggeredBy) {
        // 2. DB에서 원본 로그 조회
        EtlJobLog originLog = jobLogRepo.findById(logId)
                .orElseThrow(() -> new RuntimeException("로그 없음"));

        // 3. originLog에서 쿼리/파라미터 등 가져와서 실제 DB 작업(재수행)
        String executedQuery = originLog.getQueryText();
        String paramsJson = originLog.getParamsJson();
        // 실제 쿼리 실행은 생략

        // 4. 재수행 로그 남김
        EtlJobRetryLog retryLog = new EtlJobRetryLog();
        retryLog.setJobId(originLog.getJobId());
        retryLog.setBatchId(originLog.getBatchId());
        retryLog.setJobLog(originLog);
        retryLog.setSourceDbId(originLog.getSourceDbId());
        retryLog.setRetriedAt(new Date());
        retryLog.setResult("SUCCESS"); // 실제 결과로 대체
        retryLog.setMessage("재수행 성공"); // 실제 메시지로 대체
        retryLog.setTriggeredBy(triggeredBy);

        // 원본 로그의 쿼리/파라미터를 그대로 기록!
        retryLog.setQueryText(originLog.getQueryText());
        retryLog.setParamsJson(originLog.getParamsJson());

        retryLogRepo.save(retryLog);
        return retryLog;
    }


    public void runSingleJob(Long jobId, Long sourceDbId) {
        EtlJob job = etlJobRepo.findById(jobId).orElseThrow();

        // DB 타입별 queryMap 불러오기 (MonitorModule 기준)
        MonitorModule module = monitorModuleRepo.findById(job.getMonitorModuleId())
                .orElseThrow(() -> new RuntimeException("모듈 없음"));

        // queryMap 호출
        Map<String, String> queryMap = module.getQueryMap();

        // 해당 sourceDb 정보 로딩
        DbConnectionInfo dbInfo = dbRepo.findById(sourceDbId).orElseThrow();

        // 실행 쿼리
        String dbType = dbInfo.getDbType();
        String query = queryMap.get(dbType);

        // 쿼리 실행
        JdbcTemplate jdbc = (JdbcTemplate) DataSourceUtil.createDataSource(dbInfo);
        jdbc.execute(query);

        // 성공 시 로그 저장
        batchService.saveJobLog(jobId, sourceDbId, true, "단건 재수행 성공");
    }


    public Page<EtlBatchLogDto> getBatchLogsGroupedPaged(Long jobId, int page, int size) {
        int startRow = page * size;
        int endRow = startRow + size;

        // 1. 배치 ID 목록만 페이징 조회 (네이티브 쿼리로)
        List<Long> batchIds = jobLogRepo.findPagedBatchIdsByJobId(jobId, startRow, endRow);

        if (batchIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // 2. 해당 배치들에 해당하는 로그만 조회
        List<EtlJobLog> logs = jobLogRepo.findByBatchIdIn(batchIds);

        // 3. 배치별 그룹핑
        Map<Long, List<EtlJobLog>> batchMap = logs.stream()
                .collect(Collectors.groupingBy(EtlJobLog::getBatchId, LinkedHashMap::new, Collectors.toList()));

        // 4. 변환
        List<EtlBatchLogDto> result = new ArrayList<>();
        for (Map.Entry<Long, List<EtlJobLog>> entry : batchMap.entrySet()) {
            Long batchId = entry.getKey();
            List<EtlJobLog> batchLogs = entry.getValue();
            Date executedAt = batchLogs.get(0).getExecutedAt();

            List<EtlJobLogDto> logDtos = batchLogs.stream().map(log -> {
                EtlJobLogDto dto = new EtlJobLogDto();
                dto.setLogId(log.getLogId());
                dto.setJobId(log.getJobId());
                dto.setSourceDbId(log.getSourceDbId());
                dto.setExecutedAt(log.getExecutedAt());
                dto.setResult(log.getResult());
                dto.setMessage(log.getMessage());
                dto.setSourceDbName(
                        dbRepo.findById(log.getSourceDbId()).map(DbConnectionInfo::getDbName).orElse("Unknown")
                );
                return dto;
            }).collect(Collectors.toList());

            EtlBatchLogDto batchDto = new EtlBatchLogDto();
            batchDto.setBatchId(batchId);
            batchDto.setExecutedAt(executedAt);
            batchDto.setLogs(logDtos);
            result.add(batchDto);
        }

        // 총 배치 수는 따로 count 쿼리를 수행해서 가져와야 함
        int total = batchRepo.countByJobId(jobId); // 또는 별도 count 쿼리 생성

        return new PageImpl<>(result, PageRequest.of(page, size), total);
    }



}