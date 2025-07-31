package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.service.EtlBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SelectQueryExecModuleHandler extends AbstractEtlModuleHandler {

    private final EtlBatchService batchService;

    public SelectQueryExecModuleHandler(@Lazy EtlBatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public boolean supports(String moduleCode) {
        return "SELECT".equalsIgnoreCase(moduleCode);
    }

    @Override
    public void handle(EtlJob job, MonitorModule module, Long batchId) throws Exception {
        Map<String, String> queryMap = job.getExtractQueries();
        String targetTable = job.getTargetTable();
        if (targetTable == null || targetTable.trim().isEmpty()) {
            throw new IllegalArgumentException("타겟 테이블명이 지정되지 않았습니다.");
        }

        DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
        String targetPw = decryptPassword(target.getPassword());
        JdbcTemplate targetJdbc = createJdbc(target, targetPw);

        processSources(job, module, batchId, (src, srcJdbc) -> {
            String dbType = src.getDbType().toUpperCase();
            String selectQuery = queryMap.get(dbType);

            if (selectQuery == null || selectQuery.trim().isEmpty()) {
                batchService.logJobResult(batchId, src.getId(), false, "쿼리문 없음");
                return;
            }
            if (!selectQuery.trim().toLowerCase().startsWith("select")) {
                batchService.logJobResult(batchId, src.getId(), false, "SELECT 쿼리만 허용");
                return;
            }

            boolean isSuccess = true;
            String error = null;

            try {
                // 1. 소스 쿼리 결과 여러 row 조회
                List<Map<String, Object>> rows = srcJdbc.queryForList(selectQuery);

                // 2. 타겟 테이블 컬럼 정보 동적 조회
                List<Map<String, Object>> columns = targetJdbc.queryForList(
                        "SELECT COLUMN_NAME, COLUMN_ID FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID",
                        targetTable.toUpperCase());
                List<String> colNames = columns.stream()
                        .map(col -> (String) col.get("COLUMN_NAME"))
                        .collect(Collectors.toList());

                String columnsSql = String.join(", ", colNames);
                String bindsSql = colNames.stream().map(c -> "?").collect(Collectors.joining(", "));
                String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", targetTable, columnsSql, bindsSql);

                // 3. row별 파라미터 매핑
                List<Object[]> paramList = new ArrayList<>();
                for (Map<String, Object> row : rows) {
                    Map<String, Object> std = new HashMap<>();
                    std.put("COLLECT_TIME", new Timestamp(System.currentTimeMillis()));
                    std.put("DB_TYPE", src.getDbType());
                    std.put("DB_NAME", src.getDbName());
                    std.put("COLLECT_SQL", selectQuery);
                    std.put("STATUS", "SUCCESS");
                    std.put("ERROR_MSG", null);

                    // select 결과 컬럼 upper-case로 넣기
                    for (String key : row.keySet()) {
                        std.put(key.toUpperCase(), row.get(key));
                    }

                    Object[] vals = colNames.stream()
                            .map(col -> std.getOrDefault(col, null))
                            .toArray();
                    paramList.add(vals);
                }

                if (!paramList.isEmpty()) {
                    targetJdbc.batchUpdate(insertSql, paramList);
                    batchService.logJobResult(batchId, src.getId(), true, "총 " + paramList.size() + "건 batch insert 완료");
                } else {
                    batchService.logJobResult(batchId, src.getId(), true, "조회결과 없음");
                }

            } catch (Exception ex) {
                log.error("[{}] Batch INSERT FAIL: {}", targetTable, ex.getMessage(), ex);
                batchService.logJobResult(batchId, src.getId(), false, "Batch insert 실패: " + ex.getMessage());
            }
        });
    }

    @Override
    protected void handleSourceError(EtlJob job, Long batchId, DbConnectionInfo src, Exception e) {
        String targetTable = job.getTargetTable();
        if (targetTable == null || targetTable.trim().isEmpty()) return;

        try {
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElse(null);
            if (target == null) return;

            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            List<Map<String, Object>> columns = targetJdbc.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_ID FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID",
                    targetTable.toUpperCase());
            List<String> colNames = columns.stream()
                    .map(col -> (String) col.get("COLUMN_NAME"))
                    .collect(Collectors.toList());

            Map<String, Object> std = new HashMap<>();
            std.put("COLLECT_TIME", new Timestamp(System.currentTimeMillis()));
            std.put("DB_TYPE", src.getDbType());
            std.put("DB_NAME", src.getDbName());
            std.put("COLLECT_SQL", "(unknown)");
            std.put("STATUS", "FAIL");
            std.put("ERROR_MSG", e.getMessage());

            List<Object> values = colNames.stream()
                    .map(col -> std.getOrDefault(col, null))
                    .collect(Collectors.toList());

            String columnsSql = String.join(", ", colNames);
            String bindsSql = colNames.stream().map(c -> "?").collect(Collectors.joining(", "));
            String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", targetTable, columnsSql, bindsSql);

            if (colNames.size() == values.size()) {
                targetJdbc.update(insertSql, values.toArray());
            }
        } catch (Exception ex) {
            // 무시
        }

        String finalMessage = src.getDescription() + " | " + e.getMessage();
        batchService.logJobResult(batchId, src.getId(), false, finalMessage);
    }

    @Override
    public void handleSingle(EtlJob job, MonitorModule module, Long batchId, DbConnectionInfo src, JdbcTemplate srcJdbc) {
        Map<String, String> queryMap = job.getExtractQueries();
        String dbType = src.getDbType().toUpperCase();
        String selectQuery = queryMap.get(dbType);
        String targetTable = job.getTargetTable();

        if (targetTable == null || targetTable.trim().isEmpty()) {
            batchService.saveJobLog(batchId, src.getId(), false, "타겟 테이블명 누락: " + dbType);
            return;
        }
        if (selectQuery == null || selectQuery.trim().isEmpty()) {
            batchService.saveJobLog(batchId, src.getId(), false, "쿼리문 누락: " + dbType);
            return;
        }
        if (!selectQuery.trim().toLowerCase().startsWith("select")) {
            batchService.saveJobLog(batchId, src.getId(), false, "SELECT 쿼리만 허용");
            return;
        }

        boolean isSuccess = true;
        String error = null;

        try {
            List<Map<String, Object>> rows = srcJdbc.queryForList(selectQuery);

            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            List<Map<String, Object>> columns = targetJdbc.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_ID FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID",
                    targetTable.toUpperCase());
            List<String> colNames = columns.stream()
                    .map(col -> (String) col.get("COLUMN_NAME"))
                    .collect(Collectors.toList());

            List<Object[]> paramList = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> std = new HashMap<>();
                std.put("COLLECT_TIME", new Timestamp(System.currentTimeMillis()));
                std.put("DB_TYPE", src.getDbType());
                std.put("DB_NAME", src.getDbName());
                std.put("COLLECT_SQL", selectQuery);
                std.put("STATUS", "SUCCESS");
                std.put("ERROR_MSG", null);

                for (String key : row.keySet()) {
                    std.put(key.toUpperCase(), row.get(key));
                }
                Object[] vals = colNames.stream()
                        .map(col -> std.getOrDefault(col, null))
                        .toArray();
                paramList.add(vals);
            }
            String columnsSql = String.join(", ", colNames);
            String bindsSql = colNames.stream().map(c -> "?").collect(Collectors.joining(", "));
            String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", targetTable, columnsSql, bindsSql);

            if (!paramList.isEmpty()) {
                targetJdbc.batchUpdate(insertSql, paramList);
                batchService.saveJobLog(batchId, src.getId(), true, "총 " + paramList.size() + "건 batch insert 완료");
            } else {
                batchService.saveJobLog(batchId, src.getId(), true, "조회결과 없음");
            }

        } catch (Exception e) {
            isSuccess = false;
            error = e.getMessage();
            batchService.saveJobLog(batchId, src.getId(), false, "실패: " + error);
        }
    }
}
