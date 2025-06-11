package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.JobListDto;
import com.javis.dongkukDBmon.Dto.JobStatusMessage;
import com.javis.dongkukDBmon.config.*;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.EtlJobLog;
import com.javis.dongkukDBmon.repository.DbConnectionInfoRepository;
import com.javis.dongkukDBmon.repository.EtlJobLogRepository;
import com.javis.dongkukDBmon.repository.EtlJobRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.javis.dongkukDBmon.config.AesUtil.decrypt;

@Service
@RequiredArgsConstructor
public class EtlJobService {

    private final EtlJobRepository etlJobRepo;
    private final DbConnectionInfoRepository dbRepo;
    private final CamelContext camelContext;
    private final EtlJobLogRepository jobLogRepo;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${aes.key}")
    private String aesKey;


    // (Spring @Service에서)
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 예시: 추출 함수
    public List<Map<String, Object>> extractRows(String sql) {
        // 이 쿼리는 Statement로 실행됨 (Tibero 호환 OK)
        return jdbcTemplate.queryForList(sql);
    }



    public void notifyJobStatus(EtlJob job) {
        // 항상 로그테이블에서 최신 1건을 기준으로 JobStatusMessage 생성!
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
    public EtlJob createJob(EtlJob job) { return etlJobRepo.save(job); }
    public EtlJob updateJob(Long id, EtlJob dto) {
        EtlJob job = etlJobRepo.findById(id).orElseThrow();
        job.setJobName(dto.getJobName());
        job.setSourceDbId(dto.getSourceDbId());
        job.setTargetDbId(dto.getTargetDbId());
        job.setExtractQuery(dto.getExtractQuery());
        job.setTargetTable(dto.getTargetTable());
        job.setSchedule(dto.getSchedule());
        job.setStatus(dto.getStatus());
        return etlJobRepo.save(job);
    }
    public EtlJob getJob(Long id) { return etlJobRepo.findById(id).orElseThrow(); }
    public List<EtlJob> listJobs() { return etlJobRepo.findAll(); }
    public List<EtlJobLog> getJobLogs(Long jobId) {
        return jobLogRepo.findByJobId(jobId); // JPA 쿼리 메서드 등 사용
    }
    public EtlJobLog getLastLog(Long jobId) {
        return jobLogRepo.findLatestLogByJobId(jobId);
    }

    public String runEtlJob(Long jobId) {
        EtlJob job = etlJobRepo.findById(jobId).orElseThrow();
        DbConnectionInfo src = dbRepo.findById(job.getSourceDbId()).orElseThrow();
        DbConnectionInfo tgt = dbRepo.findById(job.getTargetDbId()).orElseThrow();

        String srcPw, tgtPw;
        try {
            srcPw = tryDecrypt(aesKey, src.getPassword());
            tgtPw = tryDecrypt(aesKey, tgt.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DB 패스워드 복호화 실패: " + e.getMessage(), e);
        }

        DataSource srcDs = DataSourceUtil.createDataSource(
                new DbConnectionInfo(src.getId(), src.getDbType(), src.getHost(), src.getPort(), src.getDbName(),
                        src.getUsername(), srcPw, src.getDescription(), src.getRegDate())
        );
        DataSource tgtDs = DataSourceUtil.createDataSource(
                new DbConnectionInfo(tgt.getId(), tgt.getDbType(), tgt.getHost(), tgt.getPort(), tgt.getDbName(),
                        tgt.getUsername(), tgtPw, tgt.getDescription(), tgt.getRegDate())
        );

        String routeId = "etl-route-" + jobId;
        String resultMessage = "";
        boolean isSuccess = false;
        final String TARGET_DATASOURCE_NAME = "targetDataSourceForJob" + jobId;

        List<Map<String, Object>> rows = List.of();
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(srcDs);
            rows = jdbcTemplate.queryForList(job.getExtractQuery());
            System.out.println("[실행되는 SELECT SQL] = [" + job.getExtractQuery() + "]");
            System.out.println("[SELECT 결과 row 수]: " + rows.size());

            // ★ Target 테이블 컬럼 메타 자동 조회!
            Map<String, TableMetaUtil.ColumnMeta> columnMetas = TableMetaUtil.getColumnMeta(tgtDs, job.getTargetTable());

            if (camelContext.getRegistry().lookupByName(TARGET_DATASOURCE_NAME) != null) {
                camelContext.getRegistry().unbind(TARGET_DATASOURCE_NAME);
            }
            camelContext.getRegistry().bind(TARGET_DATASOURCE_NAME, tgtDs);

            if (camelContext.getRoute(routeId) != null) {
                camelContext.removeRoute(routeId);
            }

            // 미리 insert SQL 만들어두기 (컬럼명 모두 ""으로 감싼 형태)
            String insertSql = SqlUtil.buildInsertSql(job.getTargetTable(), columnMetas);

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:" + routeId)
                            .split(body())
                            .process(exchange -> {
                                Map row = exchange.getIn().getBody(Map.class);
                                // ★ 자동 변환 적용!
                                Map<String, Object> converted = TypeConvertUtil.convertRow(row, columnMetas);

                                // [디버깅 로그: 변환된 값 전체]
                                System.out.println("==== [ETL INSERT PARAMS] ====");
                                for (Map.Entry<String, Object> entry : converted.entrySet()) {
                                    Object v = entry.getValue();
                                    System.out.printf("  > %s = %s (%s)%n", entry.getKey(), v, v == null ? "null" : v.getClass().getName());
                                }
                                System.out.println("============================");

                                exchange.getIn().setBody(converted);

                                // [디버깅 로그: INSERT SQL, 파라미터]
                                System.out.println("최종 실행 INSERT SQL Template: " + insertSql);
                                System.out.println("INSERT row (parameters): " + converted);

                                // [진짜로 insert 전에 중단점 찍고 싶으면 여기!]
                                // java debugger (IntelliJ, Eclipse 등) 이 라인에 브레이크포인트 걸면, 파라미터 다 볼 수 있음.
                            })
                            .doTry()
                            .toD("sql:" + insertSql + "?dataSource=#" + TARGET_DATASOURCE_NAME)
                            .process(exchange -> {
                                // [성공 시 바로 select로 확인]
                                Map<String, Object> param = exchange.getIn().getBody(Map.class);
                                System.out.println("[SUCCESS] ETL insert, params: " + param);
                            })
                            .doCatch(Exception.class)
                            .process(exchange -> {
                                // 실패 row, 모든 컬럼/타입 로그
                                Map<String, Object> failedRow = exchange.getIn().getBody(Map.class);
                                System.err.println("==== [ETL INSERT FAIL - Row 상세정보] ====");
                                for (Map.Entry<String, Object> entry : failedRow.entrySet()) {
                                    Object v = entry.getValue();
                                    System.err.printf("  > %s = %s (%s)%n", entry.getKey(), v, v == null ? "null" : v.getClass().getName());
                                }
                                System.err.println("========================================");
                                // Exception을 그대로 throw 해서 잡히게!
                                Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                                if (ex != null) throw ex;
                                // 예외가 없으면 새로운 Exception 생성해서 throw
                                throw new RuntimeException("ETL INSERT FAIL, see logs for details");
                            })
                            .end()
                            .routeId(routeId)
                            .end();
                }
            });



            ProducerTemplate template = camelContext.createProducerTemplate();
            template.sendBody("direct:" + routeId, rows);

            isSuccess = true;
            resultMessage = "ETL 실행 성공";
            job.setLastRunAt(new Date());
            job.setLastResult("SUCCESS");
            etlJobRepo.save(job);
            notifyJobStatus(job);

        } catch (Exception e) {
            e.printStackTrace();
            // 만약 rows 가 있다면, 마지막 insert 시도한 row까지 찍기 (필요시)
            System.err.println("[ETL 전체 실패, 마지막 row]: " + (rows.isEmpty() ? "없음" : rows.get(rows.size() - 1)));
            String rootMsg = getRootCauseMessage(e);
            resultMessage = "실패: " + rootMsg;
            job.setLastRunAt(new Date());
            job.setLastResult("FAIL: " + rootMsg);
            etlJobRepo.save(job);
            notifyJobStatus(job);

        } finally {
            try {
                System.out.println("[FINALLY] Route/Registry/DataSource cleanup 시작");
                if (camelContext.getRoute(routeId) != null) {
                    System.out.println("[FINALLY] removeRoute 호출");
                    camelContext.removeRoute(routeId);
                }
                if (camelContext.getRegistry().lookupByName(TARGET_DATASOURCE_NAME) != null) {
                    System.out.println("[FINALLY] registry unbind 호출");
                    camelContext.getRegistry().unbind(TARGET_DATASOURCE_NAME);
                }
                // 👇 아래 두 줄은 삭제 (공유 풀 close 금지!)
                // if (srcDs instanceof HikariDataSource) ((HikariDataSource) srcDs).close();
                // if (tgtDs instanceof HikariDataSource) ((HikariDataSource) tgtDs).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            saveJobLog(jobId, isSuccess, resultMessage);
            notifyJobStatus(job);
        }

        return resultMessage;
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
            EtlJobLog lastLog = jobLogRepo.findLatestLogByJobId(job.getId());
            return new JobListDto(job, lastLog);
        }).collect(Collectors.toList());
    }


    private void saveJobLog(Long jobId, boolean isSuccess, String message) {
        EtlJobLog log = new EtlJobLog();
        log.setJobId(jobId);
        log.setExecutedAt(new Date());
        log.setResult(isSuccess ? "SUCCESS" : "FAIL");
        log.setMessage(message);
        jobLogRepo.save(log);
    }
}