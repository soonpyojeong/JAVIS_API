package com.javis.dongkukDBmon.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class DbStatusNotifierService {
    private final SimpMessagingTemplate messagingTemplate;

    public DbStatusNotifierService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyStatusUpdate(String dbName) {
        // 간단한 JSON 구조로 알림 전송 (문자열도 가능)
        Map<String, Object> payload = Map.of(
                "refresh", true,
                "db", dbName
        );

        messagingTemplate.convertAndSend("/topic/db-status", payload);
    }
}