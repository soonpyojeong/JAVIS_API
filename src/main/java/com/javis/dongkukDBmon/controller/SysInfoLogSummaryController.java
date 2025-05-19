package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.SysInfoLogSummary;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import com.javis.dongkukDBmon.repository.SysInfoLogSummaryRepository;
import com.javis.dongkukDBmon.repository.SysInfoSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/sysinfo/log-summary")
@RequiredArgsConstructor
public class SysInfoLogSummaryController {

    private final SysInfoLogSummaryRepository summaryRepo;
    private final SysInfoSummaryRepository sysInfoSummaryRepository;

    // ✅ summaryId로 로그 요약 조회 (기존)
    @GetMapping
    public ResponseEntity<List<SysInfoLogSummary>> getSummaryBySummaryId(@RequestParam Long summaryId) {
        List<SysInfoLogSummary> results = summaryRepo.findBySummaryId(summaryId);
        return ResponseEntity.ok(results);
    }

    // ✅ hostname + 날짜로 로그 요약 조회 (프론트 달력 기능용)
    @GetMapping("/by-date")
    public ResponseEntity<List<SysInfoLogSummary>> getSummaryByHostnameAndDate(
            @RequestParam String hostname,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        SysInfoSummary summary = sysInfoSummaryRepository.findTopByHostnameAndDate(hostname, formattedDate);

        if (summary == null) return ResponseEntity.noContent().build();

        List<SysInfoLogSummary> results = summaryRepo.findBySummaryId(summary.getId());
        return ResponseEntity.ok(results);
    }
}
