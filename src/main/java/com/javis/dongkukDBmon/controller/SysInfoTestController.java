package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.SysInfoDTO;
import com.javis.dongkukDBmon.model.SysInfoDisk;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import com.javis.dongkukDBmon.repository.SysInfoDiskRepository;
import com.javis.dongkukDBmon.repository.SysInfoSummaryRepository;
import com.javis.dongkukDBmon.service.SysInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/sysinfo")
@RequiredArgsConstructor
public class SysInfoTestController {

    private final SysInfoSummaryRepository summaryRepo;
    private final SysInfoDiskRepository diskRepo;
    private final SysInfoService sysInfoService;

    @PostMapping
    public ResponseEntity<Void> receiveSysInfo(@RequestBody SysInfoDTO dto) {
        log.info("[RECEIVED] SysInfoDTO: {}", dto.getHostInfo().getHostname());

        // 1. Summary 저장
        SysInfoSummary summary = summaryRepo.save(dto.toEntity());

        // 2. Disk 정보 저장
        dto.getDiskInfo().forEach(disk -> {
            SysInfoDisk entity = new SysInfoDisk();
            entity.setSummaryId(summary.getId());
            entity.setFilesystem(disk.getFilesystem());
            entity.setDiskSize(disk.getSize());
            entity.setUsed(disk.getUsed());
            entity.setAvail(disk.getAvail());
            entity.setUsePercent(disk.getUsePercent());
            entity.setMountedOn(disk.getMountedOn());
            diskRepo.save(entity);
        });

        return ResponseEntity.ok().build();
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestByHostname(@RequestParam(required = false) String hostname) {
        SysInfoSummary latest;

        if (hostname != null) {
            latest = summaryRepo.findTopByHostnameOrderByCheckDateDesc(hostname);
        } else {
            latest = summaryRepo.findLatestSummary();
        }

        if (latest == null) return ResponseEntity.noContent().build();

        List<SysInfoDisk> disks = diskRepo.findBySummaryId(latest.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", latest);
        result.put("disks", disks);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/hostnames")
    public ResponseEntity<List<Map<String, Object>>> getAllHostnames() {
        List<Object[]> hostRows = summaryRepo.findDistinctHostnames();
        List<Map<String, Object>> result = new ArrayList<>();
        long id = 1L;

        for (Object[] row : hostRows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id++);
            map.put("loc", row[0]);
            map.put("hostname", row[1]);
            result.add(map);
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

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("disks", disks);

        return ResponseEntity.ok(result);
    }
}
