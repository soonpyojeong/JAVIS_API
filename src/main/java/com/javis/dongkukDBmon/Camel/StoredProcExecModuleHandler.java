package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.service.EtlBatchService;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StoredProcExecModuleHandler extends AbstractEtlModuleHandler {

    private final EtlBatchService batchService;
    private final InsertQueryRegistry insertQueryRegistry;

    public StoredProcExecModuleHandler(@Lazy EtlBatchService batchService, InsertQueryRegistry insertQueryRegistry) {
        this.batchService = batchService;
        this.insertQueryRegistry = insertQueryRegistry;
    }

    @Override
    public boolean supports(String moduleCode) {
        return "PROC".equalsIgnoreCase(moduleCode);
    }

    @Override
    public void handle(EtlJob job, MonitorModule module, Long batchId) throws Exception {
        Map<String, String> procMap = job.getExtractQueries();
        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
        String targetPw = decryptPassword(target.getPassword());
        JdbcTemplate targetJdbc = createJdbc(target, targetPw);

        processSources(job, module, batchId, (src, jdbc) -> {
            String dbType = src.getDbType().toUpperCase();
            String procName = procMap.get(dbType);

            if (procName == null || procName.trim().isEmpty()) {
                batchService.logJobResult(batchId, src.getId(), false, "프로시저명 없음");
                return;
            }

            boolean isSuccess = true;
            String error = null;
            String callSql = procName.trim().toUpperCase().startsWith("{CALL") ? procName.trim() : "{call " + procName.trim() + "}";

            try {
                jdbc.update(callSql);
            } catch (Exception ex) {
                isSuccess = false;
                error = ex.getMessage();
            }

            try {
                String insertSql = insertQueryRegistry.getQuery("PROC", dbType);
                if (insertSql == null || insertSql.trim().isEmpty()) {
                    batchService.logJobResult(batchId, src.getId(), false, "타겟 INSERT 쿼리 미정의: " + dbType);
                    return;
                }

                targetJdbc.update(insertSql,
                        new java.sql.Timestamp(System.currentTimeMillis()),
                        src.getDbType(),
                        src.getDbName(),
                        procName,
                        isSuccess ? "SUCCESS" : "FAIL",
                        error);

                String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
                batchService.logJobResult(batchId, src.getId(), isSuccess, finalMessage);
            } catch (Exception ex) {
                batchService.logJobResult(batchId, src.getId(), false, "타겟 DB INSERT 오류: " + ex.getMessage());
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

            String insertSql = insertQueryRegistry.getQuery("PROC", src.getDbType().toUpperCase());
            if (insertSql != null && !insertSql.isBlank()) {
                targetJdbc.update(insertSql,
                        new java.sql.Timestamp(System.currentTimeMillis()),
                        src.getDbType(),
                        src.getDbName(),
                        "(unknown)",
                        "FAIL",
                        e.getMessage());
            }
        } catch (Exception ex) {
            // 무시
        }

        String finalMessage = src.getDescription() + " | " + e.getMessage();
        batchService.logJobResult(batchId, src.getId(), false, finalMessage);
    }

    @Override
    public void handleSingle(EtlJob job, MonitorModule module, Long batchId, DbConnectionInfo src, JdbcTemplate jdbc) {
        Map<String, String> procMap = job.getExtractQueries();
        String dbType = src.getDbType().toUpperCase();
        String procName = procMap.get(dbType);

        if (procName == null || procName.trim().isEmpty()) {
            batchService.saveJobLog(batchId, src.getId(), false, "프로시저명 누락: " + dbType);
            return;
        }

        boolean isSuccess = true;
        String error = null;

        String callSql = procName.trim().toUpperCase().startsWith("{CALL") ? procName.trim() : "{call " + procName.trim() + "}";

        try {
            jdbc.update(callSql);
        } catch (Exception e) {
            isSuccess = false;
            error = e.getMessage();
        }

        try {
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            String insertSql = insertQueryRegistry.getQuery("PROC", dbType);
            if (insertSql == null || insertSql.isBlank()) {
                batchService.saveJobLog(batchId, src.getId(), false, "타겟 INSERT 쿼리 미정의: " + dbType);
                return;
            }

            targetJdbc.update(insertSql,
                    new java.sql.Timestamp(System.currentTimeMillis()),
                    src.getDbType(),
                    src.getDbName(),
                    procName,
                    isSuccess ? "SUCCESS" : "FAIL",
                    error);

        } catch (Exception e) {
            isSuccess = false;
            error = "타겟 DB 오류: " + e.getMessage();
        }

        String finalMessage = isSuccess ? src.getDescription() : (src.getDescription() + " | " + error);
        batchService.saveJobLog(batchId, src.getId(), isSuccess, finalMessage);
    }
}
