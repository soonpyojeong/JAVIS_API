package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.service.DbStatusNotifierService;
import com.javis.dongkukDBmon.service.EtlBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HealthModuleHandler extends AbstractEtlModuleHandler {

    private final EtlBatchService batchService;
    private final InsertQueryRegistry insertQueryRegistry;

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    private final DbStatusNotifierService dbStatusNotifierService;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    public HealthModuleHandler(@Lazy EtlBatchService batchService,
                               InsertQueryRegistry insertQueryRegistry,
                               SimpMessagingTemplate messagingTemplate,
                               DbStatusNotifierService dbStatusNotifierService) {
        this.batchService = batchService;
        this.insertQueryRegistry = insertQueryRegistry;
        this.messagingTemplate = messagingTemplate;
        this.dbStatusNotifierService = dbStatusNotifierService;
    }

    @Override
    public boolean supports(String moduleCode) {
        return "HEALTH".equalsIgnoreCase(moduleCode);
    }

    @Override
    public void handle(EtlJob job, MonitorModule module, Long batchId) throws Exception {
        Map<String, String> queryMap = job.getExtractQueries();
        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
        String targetPw = decryptPassword(target.getPassword());
        JdbcTemplate targetJdbc = createJdbc(target, targetPw);

        try {
            // --- 본 처리 ---
            processSources(job, module, batchId, (src, jdbc) -> {
                String dbType = src.getDbType().toUpperCase();
                String query = queryMap.get(dbType);

                if (query == null) {
                    batchService.logJobResult(batchId, src.getId(), false, "쿼리 없음");
                    return;
                }

                String message = "정상";
                String error = null;
                boolean isSuccess = true;

                int maxRetry = 2;
                int attempt = 0;
                while (attempt < maxRetry) {
                    try {
                        List<Map<String, Object>> rows = jdbc.queryForList(query);
                        break; // 성공 시 루프 탈출
                    } catch (Exception e) {
                        attempt++;
                        if (attempt == maxRetry) {
                            isSuccess = false;
                            message = "에러";
                            error = e.getMessage();
                        } else {
                            log.warn("쿼리 실패, 재시도 중... ({}/{}) - {}", attempt, maxRetry, src.getDbName());
                            Thread.sleep(500);
                        }
                    }
                }

                try {
                    String insertSql = insertQueryRegistry.getQuery("HEALTH");
                    targetJdbc.update(insertSql,
                            src.getDbType(),
                            src.getDbName(),
                            src.getDescription(),
                            message,
                            error);

                    String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
                    batchService.logJobResult(batchId, src.getId(), isSuccess, finalMessage);
                } catch (Exception e) {
                    batchService.logJobResult(batchId, src.getId(), false, "타겟 DB 오류: " + e.getMessage());
                }
            });
        } finally {
            // --- 처리 종료 시 단 한 번만 WS 전송 ---
            sendHealthWsOnce();
        }
    }

    @Override
    protected void handleSourceError(EtlJob job, Long batchId, DbConnectionInfo src, Exception e) {
        try {
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElse(null);
            if (target != null) {
                String targetPw = decryptPassword(target.getPassword());
                JdbcTemplate targetJdbc = createJdbc(target, targetPw);

                String insertSql = insertQueryRegistry.getQuery("HEALTH");
                if (insertSql != null && !insertSql.isBlank()) {
                    targetJdbc.update(insertSql,
                            src.getDbType(),
                            src.getDbName(),
                            src.getDescription(),
                            "연결 실패",
                            e.getMessage());
                    dbStatusNotifierService.notifyStatusUpdate(src.getDbName());
                }
            }
        } catch (Exception ex) {
            // 무시
        }

        String finalMessage = src.getDescription() + " | " + e.getMessage();
        batchService.logJobResult(batchId, src.getId(), false, finalMessage);
    }

    @Override
    public void handleSingle(EtlJob job, MonitorModule module, Long batchId, DbConnectionInfo src, JdbcTemplate jdbc) {
        Map<String, String> queryMap = job.getExtractQueries();
        String dbType = src.getDbType().toUpperCase();
        String query = queryMap.get(dbType);

        if (query == null || query.isBlank()) {
            batchService.saveJobLog(batchId, src.getId(), false, "추출 쿼리 누락: " + dbType);
            // 단건 처리도 종료 시 한 번만 전송
            sendHealthWsOnce();
            return;
        }

        String message = "정상";
        String error = null;
        boolean isSuccess = true;

        try {
            int maxRetry = 2;
            int attempt = 0;
            while (attempt < maxRetry) {
                try {
                    List<Map<String, Object>> rows = jdbc.queryForList(query);
                    break;
                } catch (Exception e) {
                    attempt++;
                    if (attempt == maxRetry) {
                        isSuccess = false;
                        message = "에러";
                        error = e.getMessage();
                    } else {
                        log.warn("쿼리 실패, 재시도 중... ({}/{}) - {}", attempt, maxRetry, src.getDbName());
                        Thread.sleep(500);
                    }
                }
            }

            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            String insertSql = insertQueryRegistry.getQuery("HEALTH");
            if (insertSql == null || insertSql.isBlank()) {
                batchService.saveJobLog(batchId, src.getId(), false, "타겟 INSERT 쿼리 미정의: " + dbType);
                return;
            }

            targetJdbc.update(insertSql,
                    src.getDbType(),
                    src.getDbName(),
                    src.getDescription(),
                    message,
                    error);
            dbStatusNotifierService.notifyStatusUpdate(src.getDbName());

            String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
            batchService.saveJobLog(batchId, src.getId(), isSuccess, finalMessage);

        } catch (Exception e) {
            isSuccess = false;
            error = "타겟 DB 오류: " + e.getMessage();
            String finalMessage = src.getDescription() + " | " + error;
            batchService.saveJobLog(batchId, src.getId(), false, finalMessage);
        } finally {
            // 단건 처리 종료 시 단 한 번만 WS 전송
            sendHealthWsOnce();
        }
    }

    /**
     * 처리 종료 시 단 한 번만 상태 메시지를 전송한다.
     */
    private void sendHealthWsOnce() {
        try {
            Map<String, Object> jsonMsg = new HashMap<>();
            jsonMsg.put("status", "OK");
            jsonMsg.put("message", "HEALTH 핸들 종료됨");
            jsonMsg.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/db-live-status", jsonMsg);

            // (옵션) 현재 세션/구독 디버깅 로그 한 번만
            log.info("[🧪 디버그] 현재 접속 중인 WebSocket 세션:");
            simpUserRegistry.getUsers().forEach(user -> {
                log.info("사용자: {}", user.getName());
                user.getSessions().forEach(session -> {
                    log.info("  세션 ID: {}", session.getId());
                    session.getSubscriptions().forEach(sub -> {
                        log.info("    구독 대상: {}", sub.getDestination());
                    });
                });
            });

            log.info("[✅ WebSocket 마지막 메시지 전송 완료] /topic/db-live-status");
        } catch (Exception e) {
            log.warn("[❌ WebSocket 마지막 메시지 전송 실패]", e);
        }
    }
}
