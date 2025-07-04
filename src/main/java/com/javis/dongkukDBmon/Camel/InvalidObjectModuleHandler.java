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
public class InvalidObjectModuleHandler extends AbstractEtlModuleHandler {

    private final EtlBatchService batchService;
    private final InsertQueryRegistry insertQueryRegistry;

    public InvalidObjectModuleHandler(@Lazy EtlBatchService batchService, InsertQueryRegistry insertQueryRegistry) {
        this.batchService = batchService;
        this.insertQueryRegistry = insertQueryRegistry;
    }

    @Override
    public boolean supports(String moduleCode) {
        return "INVALID_OBJECT".equalsIgnoreCase(moduleCode);
    }

    @Override
    public void handle(EtlJob job, MonitorModule module, Long batchId) throws Exception {
        Map<String, String> queryMap = module.getQueryMap();
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

            try {
                List<Map<String, Object>> result = jdbc.queryForList(query);
                String insertSql = insertQueryRegistry.getQuery("INVALID_OBJECT", dbType);

                for (Map<String, Object> row : result) {
                    targetJdbc.update(insertSql,
                            src.getDbType(),
                            src.getDbName(),
                            row.get("OWNER"),
                            row.get("OBJECT_NAME"),
                            row.get("OBJECT_TYPE")
                    );
                }

                batchService.logJobResult(batchId, src.getId(), true, "정상");

            } catch (Exception e) {
                batchService.logJobResult(batchId, src.getId(), false, e.getMessage());
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

            String insertSql = insertQueryRegistry.getQuery("INVALID_OBJECT", src.getDbType().toUpperCase());
            if (insertSql != null && !insertSql.isBlank()) {
                targetJdbc.update(insertSql,
                        src.getDbType(),
                        src.getDbName(),
                        "(unknown)",
                        "(unknown)",
                        "(unknown)");
            }
        } catch (Exception ex) {
            // 무시
        }

        batchService.logJobResult(batchId, src.getId(), false, src.getDescription() + " | " + e.getMessage());
    }

    @Override
    public void handleSingle(EtlJob job, MonitorModule module, Long batchId,
                             DbConnectionInfo src, JdbcTemplate jdbc) throws Exception {

        String dbType = src.getDbType().toUpperCase();
        String query = module.getQueryMap().get(dbType);

        if (query == null) {
            batchService.logJobResult(batchId, src.getId(), false, "쿼리 없음");
            return;
        }

        try {
            List<Map<String, Object>> result = jdbc.queryForList(query);
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);
            String insertSql = insertQueryRegistry.getQuery("INVALID_OBJECT", dbType);

            for (Map<String, Object> row : result) {
                targetJdbc.update(insertSql,
                        src.getDbType(),
                        src.getDbName(),
                        row.get("OWNER"),
                        row.get("OBJECT_NAME"),
                        row.get("OBJECT_TYPE")
                );
            }

            batchService.logJobResult(batchId, src.getId(), true, "정상");

        } catch (Exception e) {
            batchService.logJobResult(batchId, src.getId(), false, e.getMessage());
        }
    }
}
