package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.EtlJobLog;
import com.javis.dongkukDBmon.service.EtlJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/etl/job")
public class EtlJobController {

    private final EtlJobService etlJobService;

    // 1. 등록
    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody EtlJob dto) {
        return ResponseEntity.ok(etlJobService.createJob(dto));
    }

    // 2. 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody EtlJob dto) {
        return ResponseEntity.ok(etlJobService.updateJob(id, dto));
    }

    // 3. 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(etlJobService.getJob(id));
    }

    // 4. 전체 목록 조회
    @GetMapping
    public ResponseEntity<?> listJobs() {
        return ResponseEntity.ok(etlJobService.getJobsWithLastLog());
    }
    @GetMapping("/{id}/logs")
    public ResponseEntity<?> getJobLogs(@PathVariable Long id) {
        return ResponseEntity.ok(etlJobService.getJobLogs(id));
    }


    @GetMapping("/{id}/last-log")
    public ResponseEntity<?> getLastLog(@PathVariable Long id) {
        EtlJobLog lastLog = etlJobService.getLastLog(id); // 서비스에서 쿼리 위임
        return ResponseEntity.ok(lastLog);
    }

    @PostMapping("/run/{id}")
    public ResponseEntity<?> runJob(@PathVariable Long id) {
        // 실제 실행 로직은 서비스로 위임
        try {
            etlJobService.runEtlJob(id);
            return ResponseEntity.ok("실행 완료!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("실행 실패: " + e.getMessage());
        }
    }


}
