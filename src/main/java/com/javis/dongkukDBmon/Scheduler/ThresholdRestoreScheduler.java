package com.javis.dongkukDBmon.Scheduler;

import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.service.AlertService;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import com.javis.dongkukDBmon.service.ThresholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ThresholdRestoreScheduler {

    private final ThresholdService thresholdService;
    private final AlertService alertService;
    private final JavisLoginUserService javisLoginUserService;

    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시에 실행
    public void autoRestoreImsiDel() {
        int count = thresholdService.restoreExpiredImsiDel();
        if (count > 0) {
            String msg = String.format("임시해제 후 3일 경과한 %d건이 자동 복구되었습니다.", count);
            Alert alert = alertService.createAlert("THRESHOLD_AUTO_RESTORE", msg);
            List<String> users = javisLoginUserService.getAllLoginIds();
            alertService.notifyUsers(alert, users);
            alertService.sendAlertToUsers(msg);
            System.out.println("[임시해제 원복] " + msg);
        }
    }
}