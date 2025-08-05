package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.service.EtlBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSetMetaData;
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

            try {
                List<Map<String, Object>> columns = targetJdbc.queryForList(
                        "SELECT COLUMN_NAME, COLUMN_ID FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID",
                        targetTable.toUpperCase());
                List<String> colNames = columns.stream()
                        .map(col -> (String) col.get("COLUMN_NAME"))
                        .collect(Collectors.toList());

                String columnsSql = String.join(", ", colNames);
                String bindsSql = colNames.stream().map(c -> "?").collect(Collectors.joining(", "));
                String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", targetTable, columnsSql, bindsSql);

                List<Object[]> paramList = new ArrayList<>();
                Timestamp collectTime = new Timestamp(System.currentTimeMillis());

                srcJdbc.query(selectQuery, rs -> {
                    while (rs.next()) {
                        Map<String, Object> std = new HashMap<>();
                        std.put("COLLECT_TIME", collectTime);
                        std.put("DB_TYPE", src.getDbType());
                        std.put("DB_NAME", src.getDbName());
                        std.put("COLLECT_SQL", selectQuery);
                        std.put("STATUS", "SUCCESS");
                        std.put("ERROR_MSG", null);

                        ResultSetMetaData meta = rs.getMetaData();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            String col = meta.getColumnName(i).toUpperCase();
                            std.put(col, rs.getObject(i));
                        }

                        Object[] vals = colNames.stream()
                                .map(col -> std.getOrDefault(col, null))
                                .toArray();
                        paramList.add(vals);
                    }
                });

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

        try {
            DbConnectionInfo target = dbRepo.findById(job.getTargetDbId()).orElseThrow();
            String targetPw = decryptPassword(target.getPassword());
            JdbcTemplate targetJdbc = createJdbc(target, targetPw);

            List<Map<String, Object>> columns = targetJdbc.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_ID FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID",
                    targetTable.toUpperCase());
            List<String> colNames = columns.stream()
                    .map(col -> (String) col.get("COLUMN_NAME"))
                    .collect(Collectors.toList());

            String columnsSql = String.join(", ", colNames);
            String bindsSql = colNames.stream().map(c -> "?").collect(Collectors.joining(", "));
            String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", targetTable, columnsSql, bindsSql);

            List<Object[]> paramList = new ArrayList<>();
            Timestamp collectTime = new Timestamp(System.currentTimeMillis());

            srcJdbc.query(selectQuery, rs -> {
                while (rs.next()) {
                    Map<String, Object> std = new HashMap<>();
                    std.put("COLLECT_TIME", collectTime);
                    std.put("DB_TYPE", src.getDbType());
                    std.put("DB_NAME", src.getDbName());
                    std.put("COLLECT_SQL", selectQuery);
                    std.put("STATUS", "SUCCESS");
                    std.put("ERROR_MSG", null);

                    ResultSetMetaData meta = rs.getMetaData();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        std.put(meta.getColumnName(i).toUpperCase(), rs.getObject(i));
                    }

                    Object[] vals = colNames.stream()
                            .map(col -> std.getOrDefault(col, null))
                            .toArray();
                    paramList.add(vals);
                }
            });

            if (!paramList.isEmpty()) {
                targetJdbc.batchUpdate(insertSql, paramList);
                batchService.saveJobLog(batchId, src.getId(), true, "총 " + paramList.size() + "건 batch insert 완료");
            } else {
                batchService.saveJobLog(batchId, src.getId(), true, "조회결과 없음");
            }

        } catch (Exception e) {
            batchService.saveJobLog(batchId, src.getId(), false, "실패: " + e.getMessage());
        }
    }
}