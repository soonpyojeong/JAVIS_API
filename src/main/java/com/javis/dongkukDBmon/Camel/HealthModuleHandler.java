package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.service.EtlBatchService;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HealthModuleHandler extends AbstractEtlModuleHandler {

    private final EtlBatchService batchService;
    private final InsertQueryRegistry insertQueryRegistry;

    public HealthModuleHandler(@Lazy EtlBatchService batchService, InsertQueryRegistry insertQueryRegistry) {
        this.batchService = batchService;
        this.insertQueryRegistry = insertQueryRegistry;
    }

    @Override
    public boolean supports(String moduleCode) {
        return "HEALTH".equalsIgnoreCase(moduleCode);
    }

    @Override
    public void handle(EtlJob job, MonitorModule module, Long batchId) throws Exception {
        Map<String, String> queryMap = job.getExtractQueries(); // DB타입별 쿼리
        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
        String targetPw = decryptPassword(target.getPassword());
        JdbcTemplate targetJdbc = createJdbc(target, targetPw); // ✅ 한 번만 생성

        processSources(job, module, (src, jdbc) -> {
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
                        error
                );

                String baseMessage = src.getDescription();
                String finalMessage = isSuccess ? baseMessage : (baseMessage + " | " + error);

                batchService.logJobResult(batchId, src.getId(), isSuccess, finalMessage);

            } catch (Exception e) {
                batchService.logJobResult(batchId, src.getId(), false, "타겟 DB 오류: " + e.getMessage());
            }
        });
    }
    public void handleSingle(EtlJob job, MonitorModule module, Long batchId, DbConnectionInfo src, JdbcTemplate jdbc) {
        Map<String, String> queryMap = job.getExtractQueries();
        String dbType = src.getDbType().toUpperCase();
        String query = queryMap.get(dbType);

        // ✅ 추출 쿼리 체크
        if (query == null || query.trim().isEmpty()) {
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
            if (insertSql == null || insertSql.trim().isEmpty()) {
                batchService.saveJobLog(batchId, src.getId(), false, "타겟 INSERT 쿼리 미정의: " + dbType);
                return;
            }

            targetJdbc.update(insertSql,
                    src.getDbType(),
                    src.getDbName(),
                    src.getDescription(),
                    message,
                    error
            );

        } catch (Exception e) {
            isSuccess = false;
            error = "타겟 DB 오류: " + e.getMessage();
        }

        // ✅ 최종 메시지 조립 및 로그 갱신
        String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
        batchService.saveJobLog(batchId, src.getId(), isSuccess, finalMessage);
    }



}
