package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.SysInfoLog;
import com.javis.dongkukDBmon.model.SysInfoLogSummary;
import com.javis.dongkukDBmon.model.SysInfoSummary;
import com.javis.dongkukDBmon.repository.SysInfoLogRepository;
import com.javis.dongkukDBmon.repository.SysInfoLogSummaryRepository;
import com.javis.dongkukDBmon.repository.SysInfoSummaryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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
        // ✅ 수정: List<SysInfoSummary> 로 받기
        List<SysInfoSummary> summaryList = (List<SysInfoSummary>) summaryMainRepo.findLatestSummaryToday();

        List<SysInfoLogSummary> result = new ArrayList<>();
        for (SysInfoSummary summary : summaryList) {
            result.addAll(generateSummaryBySummaryId(summary.getId()));
        }

        return result; // ✅ 빠뜨리면 안됨
    }



    @Transactional
    public List<SysInfoLogSummary> generateSummaryBySummaryId(Long summaryId) {
        List<SysInfoLog> logs = logRepo.findBySummaryId(summaryId);

        if (logs.isEmpty()) return Collections.emptyList();

        List<SysInfoLogSummary> summaries = new ArrayList<>();

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
                    summaries.add(SysInfoLogSummary.builder()
                            .summaryId(summaryId)
                            .logDate(logDate)
                            .logType(logType)
                            .message(entry.getKey())
                            .count(entry.getValue())
                            .build());
                }
            }
        }

        return summaryRepo.saveAll(summaries);
    }
}
