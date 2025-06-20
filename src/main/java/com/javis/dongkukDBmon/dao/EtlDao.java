package com.javis.dongkukDBmon.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javis.dongkukDBmon.Dto.MappingRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;


@Slf4j
@Component
public class EtlDao {

    private final JdbcTemplate jdbcTemplate;
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public EtlDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    // 샘플 row에서 컬럼별 Java 타입 추출 (간단 버전)
    public Map<String, String> getColumnTypes(Map<String, Object> row) {
        Map<String, String> types = new LinkedHashMap<>();
        if (row == null) return types;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            Object val = entry.getValue();
            types.put(entry.getKey(), val == null ? "null" : val.getClass().getSimpleName());
        }
        return types;
    }

    // 1. 소스 테이블에서 샘플 데이터 N건 select
    // 1. DataSource를 직접 지정하는 경우
    // DataSource + 쿼리 + 건수 + dbType 지원
    public List<Map<String, Object>> getSampleRows(DataSource ds, String extractQuery, int sampleCount, String dbType) {
        String db = dbType != null ? dbType.toUpperCase() : "";
        String limitedQuery;

        if ("ORACLE".equals(db) || "TIBERO".equals(db)) {
            limitedQuery = "SELECT * FROM (" + extractQuery + ") WHERE ROWNUM <= " + sampleCount;
        } else if ("MYSQL".equals(db) || "MARIADB".equals(db) || "POSTGRESQL".equals(db)
                || "HANADB".equals(db) || "SYBASE".equals(db) || "EDB".equals(db)) {
            limitedQuery = extractQuery + " LIMIT " + sampleCount;
        } else if ("MSSQL".equals(db) || "SQLSERVER".equals(db)) {
            limitedQuery = "SELECT TOP " + sampleCount + " * FROM (" + extractQuery + ") T";
        } else {
            limitedQuery = extractQuery;
        }

        log.info("[샘플쿼리 실행] DataSource 직접지정, DB타입: {}, 쿼리: {}", db, limitedQuery);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(limitedQuery);
            log.info("[샘플쿼리 결과] row 수: {}", result.size());
            if (!result.isEmpty()) log.info("[샘플 첫 row] {}", result.get(0));
            return result;
        } catch (Exception e) {
            log.error("[샘플쿼리 실행 실패]", e);
            throw e;
        }
    }



    // 2. 실제 추출 쿼리 실행
    public List<Map<String, Object>> getRowsForJob(String tableName, String query) {
        // 보안상 쿼리 validate 필요! (여긴 단순 샘플)
        return jdbcTemplate.queryForList(query);
    }

    // 3. insert into target table
    public void insertRow(String tgtTable, Map<String, Object> tgtRow) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tgtTable).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
        List<Object> params = new ArrayList<>();

        int i = 0;
        for (String key : tgtRow.keySet()) {
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(key);
            values.append("?");
            params.add(tgtRow.get(key));
            i++;
        }
        sql.append(")").append(values).append(")");
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    // ⭐️ 실제 구현으로 교체!
    public List<MappingRule> parseMappingJson(String json) {
        try {
            if (json == null || json.trim().isEmpty()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<MappingRule>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Map<String, Object> applyMapping(Map<String, Object> srcRow, List<MappingRule> mappingRules) {
        Map<String, Object> tgtRow = new LinkedHashMap<>();
        for (MappingRule rule : mappingRules) {
            Object srcVal = srcRow.get(rule.getSourceColumn());
            Object tgtVal = applyTransform(srcVal, rule.getTransform(), rule.getDefaultValue());
            tgtRow.put(rule.getTargetColumn(), tgtVal);
        }
        return tgtRow;
    }

    public Object applyTransform(Object srcVal, String transform, String defaultValue) {
        if (srcVal == null || srcVal.toString().trim().isEmpty()) {
            // 값이 null이고, 디폴트값 있으면 디폴트값 반환
            return defaultValue;
        }
        if (transform == null || transform.isEmpty()) return srcVal;
        try {
            switch (transform.toLowerCase()) {
                case "to_number":
                    return Double.parseDouble(srcVal.toString());
                case "to_integer":
                    return Integer.parseInt(srcVal.toString());
                case "to_long":
                    return Long.parseLong(srcVal.toString());
                case "to_date":
                    // 날짜 변환은 실제 포맷에 따라 추가 구현 필요!
                    return java.sql.Date.valueOf(srcVal.toString().substring(0, 10));
                default:
                    return srcVal; // 변환 불가시 원본 반환
            }
        } catch (Exception e) {
            return null; // 변환 실패 시 null 처리
        }
    }

    public String checkTypeMatch(Object tgtVal, String targetType) {
        if (tgtVal == null) return "값이 null";
        String javaType = tgtVal.getClass().getSimpleName().toLowerCase();
        String tgtType = targetType == null ? "" : targetType.toLowerCase();
        if (tgtType.contains("char") && !(tgtVal instanceof String)) {
            return "문자열 타입 필요";
        }
        if (tgtType.contains("number") && !(tgtVal instanceof Number)) {
            return "숫자 타입 필요";
        }
        if ((tgtType.contains("date") || tgtType.contains("timestamp"))
                && !(tgtVal instanceof java.util.Date)) {
            return "날짜 타입 필요";
        }
        return ""; // warning 없음
    }

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }


}
