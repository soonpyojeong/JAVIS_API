
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
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
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
            messagingTemplate.convertAndSend("/topic/db-live-status", "💡HEALTH 핸들 시작됨");
            log.info("[✅ WebSocket 메시지 핸들 시작됨] /topic/db-live-status");
        } catch (Exception e) {
            log.warn("[❌ WebSocket 메시지 전송 실패]", e);
        }


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

            try {
                List<Map<String, Object>> rows = jdbc.queryForList(query);
            } catch (Exception e) {
                isSuccess = false;
                message = "에러";
                error = e.getMessage();
            }

            try {
                String insertSql = insertQueryRegistry.getQuery("HEALTH", dbType);

                targetJdbc.update(insertSql,
                        src.getDbType(),
                        src.getDbName(),
                        src.getDescription(),
                        message,
                        error);
                try {
                    Map<String, Object> jsonMsg = new HashMap<>();
                    jsonMsg.put("status", "OK");
                    jsonMsg.put("message", "HEALTH 핸들 시작됨");
                    jsonMsg.put("timestamp", System.currentTimeMillis());

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // 구독 타이밍 확보
                            messagingTemplate.convertAndSend("/topic/db-live-status", jsonMsg);
                        } catch (Exception e) {
                            log.warn("❌ 메시지 전송 실패", e);
                        }
                    }).start();

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

                    log.info("[✅ WebSocket 정상 메시지 전송 완료] /topic/db-live-status");
                } catch (Exception e) {
                    log.warn("[❌ WebSocket 메시지 전송 실패]", e);
                }
                String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
                batchService.logJobResult(batchId, src.getId(), isSuccess, finalMessage);

            } catch (Exception e) {
                batchService.logJobResult(batchId, src.getId(), false, "타겟 DB 오류: " + e.getMessage());
                try {
                    Map<String, Object> jsonMsg = new HashMap<>();
                    jsonMsg.put("status", "OK");
                    jsonMsg.put("message", "HEALTH 핸들 시작됨");
                    jsonMsg.put("timestamp", System.currentTimeMillis());
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // 구독 타이밍 확보
                            messagingTemplate.convertAndSend("/topic/db-live-status", jsonMsg);
                        } catch (Exception ee) {
                            log.warn("❌ 메시지 전송 실패", ee);
                        }
                    }).start();

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

                    log.info("[✅ WebSocket 정상 메시지 전송 완료] /topic/db-live-status");
                } catch (Exception ee) {
                    log.warn("[❌ WebSocket 메시지 전송 실패]", ee);
                }
            }
        });
    }

    @Override
    protected void handleSourceError(EtlJob job, Long batchId, DbConnectionInfo src, Exception e) {
        try {
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElse(null);
            if (target == null) return;

            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            String dbType = src.getDbType().toUpperCase();
            String insertSql = insertQueryRegistry.getQuery("HEALTH", dbType);
            if (insertSql != null && !insertSql.isBlank()) {
                targetJdbc.update(insertSql,
                        src.getDbType(),
                        src.getDbName(),
                        src.getDescription(),
                        "연결 실패",
                        e.getMessage());
                dbStatusNotifierService.notifyStatusUpdate(src.getDbName());
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
            return;
        }

        String message = "정상";
        String error = null;
        boolean isSuccess = true;

        try {
            List<Map<String, Object>> rows = jdbc.queryForList(query);
        } catch (Exception e) {
            isSuccess = false;
            message = "에러";
            error = e.getMessage();
        }

        try {
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            String insertSql = insertQueryRegistry.getQuery("HEALTH", dbType);
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
            try {
                Map<String, Object> jsonMsg = new HashMap<>();
                jsonMsg.put("status", "OK");
                jsonMsg.put("message", "HEALTH 핸들 시작됨");
                jsonMsg.put("timestamp", System.currentTimeMillis());

                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 구독 타이밍 확보
                        messagingTemplate.convertAndSend("/topic/db-live-status", jsonMsg);
                    } catch (Exception e) {
                        log.warn("❌ 메시지 전송 실패", e);
                    }
                }).start();

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

                log.info("[✅ WebSocket 정상 메시지 전송 완료] /topic/db-live-status");
            } catch (Exception e) {
                log.warn("[❌ WebSocket 메시지 전송 실패]", e);
            }
        } catch (Exception e) {
            isSuccess = false;
            error = "타겟 DB 오류: " + e.getMessage();
            try {
                Map<String, Object> jsonMsg = new HashMap<>();
                jsonMsg.put("status", "OK");
                jsonMsg.put("message", "HEALTH 핸들 시작됨");
                jsonMsg.put("timestamp", System.currentTimeMillis());

                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 구독 타이밍 확보
                        messagingTemplate.convertAndSend("/topic/db-live-status", jsonMsg);
                    } catch (Exception ee) {
                        log.warn("❌ 메시지 전송 실패", ee);
                    }
                }).start();

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

                log.info("[✅ WebSocket 정상 메시지 전송 완료] /topic/db-live-status");
            } catch (Exception ee) {
                log.warn("[❌ WebSocket 메시지 전송 실패]", ee);
            }
        }

        String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
        batchService.saveJobLog(batchId, src.getId(), isSuccess, finalMessage);
    }


}
