package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.SysInfoDTO;
import com.javis.dongkukDBmon.model.SysInfoDisk;
import com.javis.dongkukDBmon.model.SysInfoLog;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import com.javis.dongkukDBmon.repository.SysInfoDiskRepository;
import com.javis.dongkukDBmon.repository.SysInfoLogRepository;
import com.javis.dongkukDBmon.repository.SysInfoSummaryRepository;
import com.javis.dongkukDBmon.service.SysInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sysinfo")
@RequiredArgsConstructor
public class SysInfoTestController {

    private final SysInfoSummaryRepository summaryRepo;
    private final SysInfoDiskRepository diskRepo;
    private final SysInfoLogRepository logRepo;
    private final SysInfoService sysInfoService;

    @PostMapping
    public ResponseEntity<Void> receiveSysInfo(@RequestBody SysInfoDTO dto) {
        log.info("[RECEIVED] SysInfoDTO: {}", dto.getHostInfo().getHostname());

        // 1. Summary 저장
        SysInfoSummary summary = summaryRepo.save(dto.toEntity());

        // 2. Disk 정보 저장
        dto.getDiskInfo().forEach(disk -> {
            SysInfoDisk entity = new SysInfoDisk();
            entity.setSummaryId(summary.getId());  // ❗ summary 객체가 아니라 ID만 저장해야 함
            entity.setFilesystem(disk.getFilesystem());
            entity.setDiskSize(disk.getSize());
            entity.setUsed(disk.getUsed());
            entity.setAvail(disk.getAvail());
            entity.setUsePercent(disk.getUsePercent());
            entity.setMountedOn(disk.getMountedOn());
            diskRepo.save(entity);
        });

        // 3. DB 에러 로그 저장
        dto.getLogCheck().getDbLogErrors().forEach((dateStr, logs) -> {
            logs.forEach(msg -> {
                SysInfoLog logEntity = new SysInfoLog();
                logEntity.setSummary(summary);
                logEntity.setLogType("DB_ERROR");
                logEntity.setLogDate(Date.valueOf(dateStr));
                logEntity.setMessage(msg);
                logRepo.save(logEntity);
            });
        });

        // 4. 로그인 실패 로그 저장
        dto.getLogCheck().getAccountFailures().forEach((dateStr, logs) -> {
            logs.forEach(msg -> {
                SysInfoLog logEntity = new SysInfoLog();
                logEntity.setSummary(summary);
                logEntity.setLogType("LOGIN_FAIL");
                logEntity.setLogDate(Date.valueOf(dateStr));
                logEntity.setMessage(msg);
                logRepo.save(logEntity);
            });
        });

        return ResponseEntity.ok().build();
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestByHostname(@RequestParam(required = false) String hostname) {
        SysInfoSummary latest;

        if (hostname != null) {
            latest = summaryRepo.findTopByHostnameOrderByCheckDateDesc(hostname);
        } else {
            latest = summaryRepo.findLatestSummary(); // 기존 방식
        }

        if (latest == null) return ResponseEntity.noContent().build();

        List<SysInfoDisk> disks = diskRepo.findBySummaryId(latest.getId());
        List<SysInfoLog> logs = logRepo.findBySummaryId(latest.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", latest);
        result.put("disks", disks);
        result.put("logs", logs);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/hostnames")
    public ResponseEntity<List<Map<String, Object>>> getAllHostnames() {
        List<String> hostnames = summaryRepo.findDistinctHostnames();

        List<Map<String, Object>> result = new ArrayList<>();
        long id = 1L;
        for (String name : hostnames) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id++);
            m.put("hostname", name);
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }
    @GetMapping("/by-date")
    public ResponseEntity<Map<String, Object>> getSysInfoByDate(
            @RequestParam String hostname,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {

        String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        SysInfoSummary summary = summaryRepo.findTopByHostnameAndDate(hostname, formattedDate);
        if (summary == null) return ResponseEntity.noContent().build();

        List<SysInfoDisk> disks = diskRepo.findBySummaryId(summary.getId());
        List<SysInfoLog> logs = logRepo.findBySummaryId(summary.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("disks", disks);
        result.put("logs", logs);

        return ResponseEntity.ok(result);
    }

}
