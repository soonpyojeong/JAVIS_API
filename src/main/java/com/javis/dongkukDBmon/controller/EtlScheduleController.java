package com.javis.dongkukDBmon.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.javis.dongkukDBmon.Dto.JobOptionDto;
import com.javis.dongkukDBmon.Dto.EtlScheduleDto;
import com.javis.dongkukDBmon.service.EtlScheduleService;
import com.javis.dongkukDBmon.repository.EtlJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.javis.dongkukDBmon.Camel.EtlSchedulerManager;

import java.util.List;

@RestController
@RequestMapping("/api/etl/schedules")
@RequiredArgsConstructor
public class EtlScheduleController {

    private final EtlScheduleService schservice;
    private final EtlJobRepository etlJobRepo;
    private final EtlSchedulerManager etlSchedulerManager;

    // 등록
    @PostMapping
    public ResponseEntity<EtlScheduleDto> save(@RequestBody EtlScheduleDto dto) throws JsonProcessingException {
        EtlScheduleDto saved = schservice.save(dto);
        try {
            etlSchedulerManager.addOrUpdateScheduleRoute(saved); // Route 동기화!
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(schservice.save(dto));
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<EtlScheduleDto> update(@PathVariable Long id, @RequestBody EtlScheduleDto dto) throws JsonProcessingException {
        EtlScheduleDto updated = schservice.update(id, dto);
        try {
            etlSchedulerManager.addOrUpdateScheduleRoute(updated); // Route 동기화!
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("수정 DTO: " + dto); // 여기서 scheduleExpr 출력!
        EtlScheduleDto updatedDto = schservice.update(id, dto);
        return ResponseEntity.ok(updatedDto);
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            etlSchedulerManager.removeScheduleRoute(id); // Route 제거!
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        schservice.delete(id);
        return ResponseEntity.ok().build();
    }

    // 전체 조회 (Entity → DTO 변환해서 리턴)
    @GetMapping
    public ResponseEntity<List<EtlScheduleDto>> list() {
        List<EtlScheduleDto> list = schservice.findAll();
        return ResponseEntity.ok(list);
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<EtlScheduleDto> get(@PathVariable Long id) {
        EtlScheduleDto dto = schservice.findById(id);
        return ResponseEntity.ok(dto);
    }

    // 스케줄러에서 사용할 JOB 리스트: /api/etl/schedules/jobs
    @GetMapping("/jobs")
    public ResponseEntity<List<JobOptionDto>> jobList() {
        List<JobOptionDto> jobList = etlJobRepo.findAll().stream()
                .map(job -> {
                    JobOptionDto jobDto = new JobOptionDto();
                    jobDto.setId(job.getId());
                    jobDto.setJobName(job.getJobName());
                    return jobDto;
                }).toList();
        return ResponseEntity.ok(jobList);
    }
}
