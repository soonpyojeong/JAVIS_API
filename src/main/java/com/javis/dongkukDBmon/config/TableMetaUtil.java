// com.javis.dongkukDBmon.config.TableMetaUtil
package com.javis.dongkukDBmon.config;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class TableMetaUtil {
    // 테이블명은 반드시 대문자, "" 없이
    // TableMetaUtil에서 scale까지 추출
    public static class ColumnMeta {
        public int dataType;        // java.sql.Types
        public int precision;       // 전체 자릿수
        public int scale;           // 소숫점 이하
        public String typeName;     // VARCHAR2/NUMBER 등 문자열 타입
        public boolean nullable;    // NULL 허용 여부
        public Integer columnSize;

        public ColumnMeta(int dataType, int precision, int scale, String typeName, boolean nullable) {
            this.dataType = dataType;
            this.precision = precision;
            this.scale = scale;
            this.typeName = typeName;
            this.nullable = nullable;
        }
    }
    public static Map<String, ColumnMeta> getColumnMeta(DataSource ds, String tableName) throws SQLException {
        Map<String, ColumnMeta> columnMetas = new HashMap<>();
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName.toUpperCase(), null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME").toUpperCase();
                    int dataType = rs.getInt("DATA_TYPE"); // java.sql.Types
                    int colSize  = rs.getInt("COLUMN_SIZE");
                    int scale = rs.getInt("DECIMAL_DIGITS");
                    String typeName = rs.getString("TYPE_NAME"); // ← 문자열 타입명
                    int nullable = rs.getInt("NULLABLE");
                    boolean isNullable = (nullable == DatabaseMetaData.columnNullable);
                    columnMetas.put(colName,
                            new ColumnMeta(dataType, colSize, scale, typeName, isNullable));
                }
            }
        }
        return columnMetas;
    }




}
