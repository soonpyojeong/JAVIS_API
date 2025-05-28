package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.SysInfoLog;
import com.javis.dongkukDBmon.model.SysInfoLogSummary;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import com.javis.dongkukDBmon.repository.SysInfoLogRepository;
import com.javis.dongkukDBmon.repository.SysInfoLogSummaryRepository;
import com.javis.dongkukDBmon.repository.SysInfoSummaryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSummarySchedulerService {

    private final SysInfoLogRepository logRepo;
    private final SysInfoLogSummaryRepository summaryRepo;
    private final SysInfoSummaryRepository summaryMainRepo;

    private String normalizeMessage(String msg) {
        if (msg == null) return "UNKNOWN";
        return msg.replaceFirst("^\\[[^\\]]+\\]\\s+\\[[^\\]]+\\]\\s+\\[[^\\]]+\\]\\s*", "").trim();
    }

    @Transactional
    public List<SysInfoLogSummary> generateTodaySummary() {
        List<SysInfoSummary> summaryList = summaryMainRepo.findLatestSummaryToday();
        if (summaryList == null || summaryList.isEmpty()) return Collections.emptyList();

        List<Long> summaryIds = summaryList.stream()
                .map(SysInfoSummary::getId)
                .toList();

        // 전체 로그 한방에 SELECT
        List<SysInfoLog> allLogs = logRepo.findBySummaryIds(summaryIds);

        // summaryId별로 그룹화해서 각각 처리
        Map<Long, List<SysInfoLog>> logMapBySummaryId = allLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getSummary().getId()));

        List<SysInfoLogSummary> result = new ArrayList<>();

        for (SysInfoSummary summary : summaryList) {
            Long summaryId = summary.getId();
            List<SysInfoLog> logs = logMapBySummaryId.getOrDefault(summaryId, Collections.emptyList());

            if (logs.isEmpty()) continue;

            // 날짜-타입-메시지별 그룹핑 및 count
            Map<String, Map<String, Map<String, Long>>> grouped = logs.stream()
                    .collect(Collectors.groupingBy(
                            log -> {
                                Date date = log.getLogDate();
                                return (date instanceof java.sql.Date)
                                        ? ((java.sql.Date) date).toLocalDate().toString()
                                        : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                            },
                            Collectors.groupingBy(
                                    SysInfoLog::getLogType,
                                    Collectors.groupingBy(
                                            log -> normalizeMessage(log.getMessage()),
                                            Collectors.counting()
                                    )
                            )
                    ));

            for (String logDate : grouped.keySet()) {
                for (String logType : grouped.get(logDate).keySet()) {
                    Map<String, Long> messageMap = grouped.get(logDate).get(logType);
                    for (Map.Entry<String, Long> entry : messageMap.entrySet()) {
                        result.add(SysInfoLogSummary.builder()
                                .summaryId(summaryId)
                                .logDate(logDate)
                                .logType(logType)
                                .message(entry.getKey())
                                .count(entry.getValue())
                                .build());
                    }
                }
            }
        }

        // 배치 인서트 적용됨: saveAll 한 번만 호출
        return summaryRepo.saveAll(result);
    }
}
