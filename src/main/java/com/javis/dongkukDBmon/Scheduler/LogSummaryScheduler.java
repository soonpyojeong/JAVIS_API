package com.javis.dongkukDBmon.Scheduler;

import com.javis.dongkukDBmon.model.SysInfoLogSummary;
import com.javis.dongkukDBmon.service.LogSummarySchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogSummaryScheduler {

    private final LogSummarySchedulerService summaryService;

    // 매일 새벽 1시에 실행
    @Scheduled(cron = "0 23 14 * * *")
    public void runDailySummary() {
        log.info("[🔄 LogSummaryScheduler] 시작");

        List<SysInfoLogSummary> count = summaryService.generateTodaySummary();
        log.info("📌 오늘 요약 결과 → {}건 저장됨", count.size());

        log.info("[✅ LogSummaryScheduler] 완료");
    }
}