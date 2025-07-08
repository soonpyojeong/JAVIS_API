package com.javis.dongkukDBmon.config;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceUtil {

    private static final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    public static DataSource createDataSource(DbConnectionInfo dbInfo) {
        String key = makeKey(dbInfo);

        return dataSourceCache.computeIfAbsent(key, k -> {
            try {
                return createRawDataSource(dbInfo);
            } catch (Exception e) {
                throw new RuntimeException("DataSource 생성 실패: " + e.getMessage(), e);
            }
        });
    }

    public static DataSource createDataSource(DbConnectionInfo dbInfo, String password) {
        DbConnectionInfo copy = new DbConnectionInfo();
        copy.setDbType(dbInfo.getDbType());
        copy.setHost(dbInfo.getHost());
        copy.setPort(dbInfo.getPort());
        copy.setDbName(dbInfo.getDbName());
        copy.setUsername(dbInfo.getUsername());
        copy.setPassword(password);
        return createDataSource(copy);
    }

    private static DataSource createRawDataSource(DbConnectionInfo d) {
        String dbType = d.getDbType().toUpperCase();

        // SYS 유저 여부
        boolean isSysUser = "ORACLE".equals(dbType) && "sys".equalsIgnoreCase(d.getUsername());

        if ("ORACLE".equals(dbType)) {
            String urlSid = "jdbc:oracle:thin:@" + d.getHost() + ":" + d.getPort() + ":" + d.getDbName();
            String urlService = "jdbc:oracle:thin:@//" + d.getHost() + ":" + d.getPort() + "/" + d.getDbName();

            Exception lastException = null;

            try {
                if (isSysUser) {
                    Properties props = new Properties();
                    props.put("user", d.getUsername());
                    props.put("password", d.getPassword());
                    props.put("internal_logon", "sysdba");
                    return buildHikari(urlSid, d.getUsername(), d.getPassword(), getDriverClassName(dbType), props);
                } else {
                    return buildHikari(urlSid, d.getUsername(), d.getPassword(), getDriverClassName(dbType));
                }
            } catch (Exception e1) {
                System.err.println("[Oracle] ⚠ SID 방식 실패 → 1초 대기 후 ServiceName 방식 재시도");
                System.err.println("[Oracle] ⚠ 원인: " + e1.getMessage());
                lastException = e1;

                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("[Oracle] Sleep 중 인터럽트 발생", ie);
                }

                try {
                    if (isSysUser) {
                        Properties props = new Properties();
                        props.put("user", d.getUsername());
                        props.put("password", d.getPassword());
                        props.put("internal_logon", "sysdba");
                        return buildHikari(urlService, d.getUsername(), d.getPassword(), getDriverClassName(dbType), props);
                    } else {
                        return buildHikari(urlService, d.getUsername(), d.getPassword(), getDriverClassName(dbType));
                    }
                } catch (Exception e2) {
                    System.err.println("[Oracle] ServiceName 방식도 실패함: " + e2.getMessage());
                    throw new RuntimeException("[Oracle] SID/ServiceName 모두 실패: " + e2.getMessage(), e2);
                }
            }
        }

        // 그 외 DB는 기존 방식
        String url = makeJdbcUrl(d);
        return buildHikari(url, d.getUsername(), d.getPassword(), getDriverClassName(dbType));
    }


    private static DataSource buildHikari(String url, String user, String pw, String driverClass) {
        return buildHikari(url, user, pw, driverClass, null);
    }

    private static DataSource buildHikari(String url, String user, String pw, String driverClass, Properties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driverClass);

        if (props != null) {
            config.setDataSourceProperties(props);
        } else {
            config.setUsername(user);
            config.setPassword(pw);
        }
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setIdleTimeout(300_000); // 유휴 커넥션 5분 후 제거
        config.setMaxLifetime(900_000); // 커넥션 최대 수명 15분
        config.setConnectionTimeout(30_000); // 커넥션 요청 제한 30초
        config.setValidationTimeout(5000);   // 커넥션 유효성 검사 제한 시간 5초
        config.setPoolName("DBMON-Pool");
        return new HikariDataSource(config);
    }




    private static String makeKey(DbConnectionInfo db) {
        return db.getDbType().toUpperCase() + ":" + db.getHost() + ":" + db.getPort() + "/" + db.getDbName();
    }

    // 🔍 기본 URL 생성기 (fallback용)
    public static String makeJdbcUrl(DbConnectionInfo d) {
        String dbType = d.getDbType().toUpperCase();
        String dbName = d.getDbName();

        return switch (dbType) {
            case "ORACLE"     -> "jdbc:oracle:thin:@" + d.getHost() + ":" + d.getPort() + ":" + dbName; // SID 방식 or 필요시 ServiceName 방식
            case "MYSQL"      -> "jdbc:mysql://" + d.getHost() + ":" + d.getPort() + "/" + dbName;
            case "MARIADB"    -> "jdbc:mariadb://" + d.getHost() + ":" + d.getPort() + "/" + dbName;
            case "MSSQL"      -> "jdbc:sqlserver://" + d.getHost() + ":" + d.getPort() + ";databaseName=" + dbName + ";encrypt=false";
            case "POSTGRESQL" -> "jdbc:postgresql://" + d.getHost() + ":" + d.getPort() + "/" + dbName;
            case "TIBERO"     -> "jdbc:tibero:thin:@" + d.getHost() + ":" + d.getPort() + ":" + dbName;
            default           -> throw new IllegalArgumentException("지원하지 않는 DB 타입: " + dbType);
        };
    }

    public static String getDriverClassName(String dbType) {
        return switch (dbType.toUpperCase()) {
            case "ORACLE"     -> "oracle.jdbc.OracleDriver";
            case "MYSQL"      -> "com.mysql.cj.jdbc.Driver";
            case "MARIADB"    -> "org.mariadb.jdbc.Driver";
            case "MSSQL"      -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "POSTGRESQL" -> "org.postgresql.Driver";
            case "TIBERO"     -> "com.tmax.tibero.jdbc.TbDriver";
            default           -> throw new IllegalArgumentException("지원하지 않는 DB 드라이버: " + dbType);
        };
    }
}
