
package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.config.AesUtil;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.service.DbStatusNotifierService;
import com.javis.dongkukDBmon.service.EtlBatchService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HealthModuleHandler extends AbstractEtlModuleHandler {

    private final EtlBatchService batchService;
    private final InsertQueryRegistry insertQueryRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final DbStatusNotifierService dbStatusNotifierService;

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
                messagingTemplate.convertAndSend("/topic/db-live-status", "OK");
                String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
                batchService.logJobResult(batchId, src.getId(), isSuccess, finalMessage);

            } catch (Exception e) {
                batchService.logJobResult(batchId, src.getId(), false, "타겟 DB 오류: " + e.getMessage());
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
        } catch (Exception e) {
            isSuccess = false;
            error = "타겟 DB 오류: " + e.getMessage();
        }

        String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
        batchService.saveJobLog(batchId, src.getId(), isSuccess, finalMessage);
    }
}
