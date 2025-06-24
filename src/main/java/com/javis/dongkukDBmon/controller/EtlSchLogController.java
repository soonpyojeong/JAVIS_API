package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.EtlJobLogDto;
import com.javis.dongkukDBmon.Dto.EtlSchLogDto;
import com.javis.dongkukDBmon.service.EtlSchLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etl/schedule-logs")
@RequiredArgsConstructor
public class EtlSchLogController {
    private final EtlSchLogService schLogService;


    // 상세 단건 조회
    @GetMapping("/{logId}")
    public EtlSchLogDto getLog(@PathVariable Long logId) {
        // schLogService.getLog(logId) 메소드 추가 필요
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // 조건 검색, 페이징 조회 등은 추후 구현 가능
    @GetMapping("/{scheduleId}/logs")
    public Page<EtlSchLogDto> findByScheduleId(
            @PathVariable Long scheduleId,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return schLogService.findByScheduleId(scheduleId, page, size);
    }




}
