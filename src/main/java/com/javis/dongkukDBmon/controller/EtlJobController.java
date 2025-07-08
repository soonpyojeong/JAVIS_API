package com.javis.dongkukDBmon.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javis.dongkukDBmon.Dto.*;
import com.javis.dongkukDBmon.mapper.MonitorModuleMapper;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.EtlJobLog;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.repository.*;
import com.javis.dongkukDBmon.Dto.RetryRequest;
import com.javis.dongkukDBmon.service.EtlBatchService;
import com.javis.dongkukDBmon.service.EtlJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/etl/job")

public class EtlJobController {

    private final EtlJobService etlJobService;
    private final DbConnectionInfoRepository dbRepo;
    private final EtlJobRepository etlJobRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MonitorModuleRepository monitorModuleRepo;
    private final EtlJobLogRepository etlJobLogRepo;
    private final EtlBatchService etlBatchService;
    private final ProducerTemplate producerTemplate;

    // 1. 등록
    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody EtlJobDto dto) {
        return ResponseEntity.ok(etlJobService.createJob(dto));
    }

    // 2. 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody EtlJobDto dto) {
        etlJobService.updateJob(id, dto);
        return ResponseEntity.ok().build(); // ✅ 리턴 없이 200 응답
    }


    // 3. 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(etlJobService.getJob(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        etlJobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    // 4. 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<EtlJobDto>> getAllJobs() {
        List<EtlJob> jobs = etlJobRepo.findAll();

        List<EtlJobDto> result = jobs.stream().map(job -> {
            EtlJobDto dto = new EtlJobDto();
            dto.setId(job.getId());
            dto.setJobName(job.getJobName());
            dto.setStatus(job.getStatus());
            dto.setTargetDbId(job.getTargetDbId());
            dto.setTargetTable(job.getTargetTable());
            dto.setMonitorModuleId(job.getMonitorModuleId());

            // 1. sourceDbIdsJson 파싱
            try {
                if (job.getSourceDbIdsJson() != null) {
                    dto.setSourceDbIds(objectMapper.readValue(
                            job.getSourceDbIdsJson(), new TypeReference<>() {
                            }
                    ));
                }
            } catch (Exception e) {
                dto.setSourceDbIds(Collections.emptyList());
            }

            // 2. extractQueryJson 파싱
            try {
                if (job.getExtractQueryJson() != null) {
                    dto.setExtractQueries(objectMapper.readValue(
                            job.getExtractQueryJson(), new TypeReference<>() {
                            }
                    ));
                }
            } catch (Exception e) {
                dto.setExtractQueries(Collections.emptyMap());
            }

            // 3. 마지막 로그 가져오기
            Optional<EtlJobLog> lastLog = Optional.ofNullable(etlJobLogRepo.findLatestLogByJobId(job.getId()));
            lastLog.ifPresent(log -> {
                Date executedAt = log.getExecutedAt();
                LocalDateTime localDateTime = executedAt.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                dto.setLastRunAt(localDateTime);        // ✅ 딱 이거 하나만!
                dto.setLastResult(log.getResult());
            });


            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // 5. 로그 조회
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<EtlJobLogDto>> getAllJobLogDtos(@PathVariable Long id) {
        return ResponseEntity.ok(etlJobService.getAllJobLogDtos(id)); // ✅ 올바른 메서드명
    }


    // 6. 마지막 로그만 조회
    @GetMapping("/{id}/last-log")
    public ResponseEntity<?> getLastLog(@PathVariable Long id) {
        EtlJobLog lastLog = etlJobService.getLastLog(id);
        return ResponseEntity.ok(lastLog);
    }

    @PostMapping("/{jobId}/retry/{sourceDbId}")
    public ResponseEntity<?> retryJob(@PathVariable Long jobId, @PathVariable Long sourceDbId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", jobId);
        payload.put("sourceDbId", sourceDbId);
        producerTemplate.sendBody("direct:runEtlJob", payload);

        return ResponseEntity.ok("재수행 요청 완료");
    }

    @PostMapping("/retry")
    public ResponseEntity<String> retryLog(@RequestBody RetryRequest request) {
        etlJobService.retryJob(request.getLogId(), request.getTriggeredBy());
        return ResponseEntity.ok("재수행 완료");
    }


    // 7. 실행
    @PostMapping("/run/{id}")
    public ResponseEntity<?> runJob(@PathVariable Long id) {
        try {
            etlJobService.runEtlJob(id);
            return ResponseEntity.ok("실행 완료!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("실행 실패: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/db-info")
    public Map<String, Object> getJobDbInfo(@PathVariable Long id) {
        EtlJob job = etlJobRepo.findById(id).orElseThrow();

        List<Long> sourceIds = new ArrayList<>();
        try {
            String json = job.getSourceDbIdsJson();
            if (json != null && !json.isBlank()) {
                sourceIds = objectMapper.readValue(json, new TypeReference<List<Long>>() {
                });
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("sourceDbIdsJson 파싱 실패", e);
        }

        List<DbConnectionInfo> sources = dbRepo.findAllById(sourceIds);
        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElse(null);

        Map<String, String> extractQueries = new HashMap<>();
        try {
            if (job.getExtractQueryJson() != null) {
                extractQueries = objectMapper.readValue(
                        job.getExtractQueryJson(),
                        new TypeReference<Map<String, String>>() {
                        }
                );
            }
        } catch (Exception e) {
            // Optional: 로그 남기기
            e.printStackTrace();
        }
        // ✅ 관제 모듈 DTO 변환
        MonitorModuleDto monitorModuleDto = null;
        if (job.getMonitorModuleId() != null) {
            MonitorModule module = monitorModuleRepo.findById(job.getMonitorModuleId()).orElse(null);
            if (module != null) {
                monitorModuleDto = MonitorModuleMapper.toDto(module);
            }
        }

        return Map.of(
                "sourceDbs", sources,
                "targetDb", target,
                "monitorModule", monitorModuleDto,
                "extractQueries", extractQueries // ✅ 프론트로 전달!
        );
    }
    @GetMapping("/{jobId}/batch-logs")
    public Page<EtlBatchLogDto> getBatchLogsPaged(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return etlJobService.getBatchLogsGroupedPaged(jobId, page, size);
    }
}

