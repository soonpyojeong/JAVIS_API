package com.javis.dongkukDBmon.Scheduler;

import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.service.AlertService;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import com.javis.dongkukDBmon.service.ThresholdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdRestoreScheduler {

    private final ThresholdService thresholdService;
    private final AlertService alertService;
    private final JavisLoginUserService javisLoginUserService;

    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시에 실행
    public void autoRestoreImsiDel() {
        log.info("[== 🕐 임시해제 원복 스케줄러 시작 (매일 1시) ==]");

        try {
            int count = thresholdService.restoreExpiredImsiDel();
            log.info("✅ 복구 대상 건수: {}", count);

            if (count > 0) {
                String msg = String.format("임시해제 후 3일 경과한 %d건이 자동 복구되었습니다.", count);
                Alert alert = alertService.createAlert("THRESHOLD_AUTO_RESTORE", msg);
                List<String> users = javisLoginUserService.getAllLoginIds();
                alertService.notifyUsers(alert, users);
                alertService.sendAlertToUsers(msg);
                log.info("📢 사용자에게 알림 전송 완료: {}", msg);
            } else {
                log.info("✅ 복구할 항목이 없어 알림 전송은 생략됨.");
            }

        } catch (Exception e) {
            log.error("❌ 임시해제 원복 스케줄러 실행 중 예외 발생", e);
        }

        log.info("[== ✅ 임시해제 원복 스케줄러 종료 ==]");
    }
}
