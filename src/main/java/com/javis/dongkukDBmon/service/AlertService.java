package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.repository.UserAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.model.UserAlert;
import com.javis.dongkukDBmon.repository.AlertRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserAlertRepository userAlertRepository;
    private final SimpMessagingTemplate messagingTemplate; // ✅ 여기서 주입

    public Alert createAlert(String type, String message) {
        Alert alert = new Alert();
        alert.setType(type);
        alert.setMessage(message);
        alert.setCreatedAt(LocalDateTime.now());

        Alert saved = alertRepository.save(alert);
        log.info("[DEBUG] 저장된 Alert ID: {}", saved.getId()); // ✅ 로그 확인
        return saved;
    }
    public void notifyUsers(Alert alert, List<String> userIds) {
        for (String userId : userIds) {
            UserAlert ua = new UserAlert();
            ua.setUserId(userId);
            ua.setAlert(alert);
            ua.setIsDeleted("N");
            userAlertRepository.save(ua);
        }
    }

    public List<UserAlert> getUserAlerts(String userId) {
        return userAlertRepository.findByUserIdAndIsDeletedOrderByAlert_CreatedAtDesc(userId, "N");
    }

    public void markAsDeleted(String userId, Long alertId) {
        UserAlert ua = userAlertRepository.findByUserIdAndAlertId(userId, alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found for user"));
        ua.setIsDeleted("Y");
        userAlertRepository.save(ua);
    }

    // ✅ 여기 추가
    public void sendAlertToUsers(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ALERT");
        payload.put("message", message);
        messagingTemplate.convertAndSend("/topic/alert", payload);
    }

}
