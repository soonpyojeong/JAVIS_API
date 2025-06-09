// com.javis.dongkukDBmon.config.TypeConvertUtil
package com.javis.dongkukDBmon.config;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class TypeConvertUtil {
    // 컬럼 메타 정보를 모두 넘겨야 함!
    public static Map<String, Object> convertRow(Map<String, Object> srcRow, Map<String, TableMetaUtil.ColumnMeta> columnMetas) {
        Map<String, Object> param = new HashMap<>();
        for (String key : srcRow.keySet()) {
            Object val = srcRow.get(key);
            TableMetaUtil.ColumnMeta meta = columnMetas.get(key.toUpperCase());
            // meta가 없으면 VARCHAR처럼 처리
            param.put(key.toUpperCase(), convertType(val, meta));
        }
        return param;
    }



    public static Object convertType(Object val, TableMetaUtil.ColumnMeta meta) {
        if (val == null || val.toString().trim().isEmpty()) return null;

        try {
            switch (meta.dataType) {
                case Types.NUMERIC:
                case Types.DECIMAL:
                    // scale=0이면 정수, scale>0면 실수
                    if (meta.scale == 0) {
                        if (val instanceof Number) return ((Number) val).longValue();
                        String s = val.toString().replaceAll(",", "");
                        if (s.contains(".")) return (long) Double.parseDouble(s); // "0.0"도 0으로!
                        return Long.parseLong(s);
                    } else {
                        if (val instanceof Number) return ((Number) val).doubleValue();
                        return Double.parseDouble(val.toString().replaceAll(",", ""));
                    }
                case Types.INTEGER:
                case Types.SMALLINT:
                case Types.TINYINT:
                    if (val instanceof Number) return ((Number) val).intValue();
                    return Integer.parseInt(val.toString().replaceAll(",", ""));
                case Types.BIGINT:
                    if (val instanceof Number) return ((Number) val).longValue();
                    return Long.parseLong(val.toString().replaceAll(",", ""));

                case Types.FLOAT:
                case Types.DOUBLE:
                    if (val instanceof Number) return ((Number) val).doubleValue();
                    return Double.parseDouble(val.toString().replaceAll(",", ""));

                case Types.DATE:
                    if (val instanceof java.sql.Date) return val;
                    if (val instanceof java.sql.Timestamp) return new java.sql.Date(((java.sql.Timestamp) val).getTime());
                    if (val instanceof java.util.Date) return new java.sql.Date(((java.util.Date) val).getTime());
                    String strVal = val.toString().replace('/', '-');
                    if (strVal.length() >= 10) return java.sql.Date.valueOf(strVal.substring(0, 10));
                    return null;

                case Types.TIMESTAMP:
                    if (val instanceof java.sql.Timestamp) return val;
                    if (val instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) val).getTime());
                    String tsStr = val.toString().replace('/', '-');
                    if (tsStr.length() == 10) tsStr += " 00:00:00";
                    tsStr = tsStr.replace('T', ' ');
                    try {
                        return java.sql.Timestamp.valueOf(tsStr);
                    } catch (Exception e) {
                        return null;
                    }

                default:
                    return val.toString();
            }
        } catch (Exception ex) {
            System.err.println("TypeConvertUtil 변환 오류: " + val + " (" + meta.dataType + ")");
            ex.printStackTrace();
            return null;
        }
    }




}
