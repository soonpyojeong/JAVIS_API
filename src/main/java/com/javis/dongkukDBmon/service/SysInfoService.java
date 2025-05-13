package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.SysInfoDTO;
import com.javis.dongkukDBmon.model.SysInfoDisk;
import com.javis.dongkukDBmon.model.SysInfoLog;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import com.javis.dongkukDBmon.repository.SysInfoDiskRepository;
import com.javis.dongkukDBmon.repository.SysInfoLogRepository;
import com.javis.dongkukDBmon.repository.SysInfoSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysInfoService {

    private final SysInfoSummaryRepository summaryRepo;
    private final SysInfoDiskRepository diskRepo;
    private final SysInfoLogRepository logRepo;

    /**
     * JSON 수신 시 저장 처리
     */
    public void saveSysInfo(SysInfoDTO dto) {
        // 1. Summary 저장
        SysInfoSummary summary = summaryRepo.save(dto.toEntity());

        // 2. 디스크 정보 저장
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

        // 3. DB 로그 저장
        dto.getLogCheck().getDbLogErrors().forEach((dateStr, logs) -> {
            logs.forEach(msg -> {
                SysInfoLog log = new SysInfoLog();
                log.setSummary(summary);
                log.setLogType("DB_ERROR");
                log.setLogDate(java.sql.Date.valueOf(dateStr));
                log.setMessage(msg);
                logRepo.save(log);
            });
        });

        // 4. 로그인 실패 로그 저장
        dto.getLogCheck().getAccountFailures().forEach((dateStr, logs) -> {
            logs.forEach(msg -> {
                SysInfoLog log = new SysInfoLog();
                log.setSummary(summary);
                log.setLogType("LOGIN_FAIL");
                log.setLogDate(java.sql.Date.valueOf(dateStr));
                log.setMessage(msg);
                logRepo.save(log);
            });
        });
    }

    /**
     * 가장 최근 수집된 시스템 정보를 조회
     */
    public Map<String, Object> getLatestSysInfo() {
        SysInfoSummary latest = summaryRepo.findLatestSummary(); // ✅ Oracle 11g 호환 nativeQuery 사용
        if (latest == null) return null;

        List<SysInfoDisk> disks = diskRepo.findBySummaryId(latest.getId());
        List<SysInfoLog> logs = logRepo.findBySummaryId(latest.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", latest);
        result.put("disks", disks);
        result.put("logs", logs);

        return result;
    }



}
