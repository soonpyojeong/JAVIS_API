package com.javis.dongkukDBmon.config;

import java.util.Map;
import java.util.stream.Collectors;

public class SqlUtil {

    // SqlUtil.java
    public static String buildInsertSql(String tableName, Map<String, TableMetaUtil.ColumnMeta> columnMetas) {
        String columns = columnMetas.keySet().stream()
                .map(k -> "\"" + k + "\"").collect(Collectors.joining(", "));
        String placeholders = columnMetas.keySet().stream()
                .map(k -> ":" + k).collect(Collectors.joining(", "));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

}