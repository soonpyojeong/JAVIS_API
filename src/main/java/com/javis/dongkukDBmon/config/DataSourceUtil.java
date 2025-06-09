package com.javis.dongkukDBmon.config;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourceUtil {

    // 🔁 DB별 커넥션 풀 캐싱
    private static final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    public static DataSource createDataSource(DbConnectionInfo dbInfo) {
        String key = makeKey(dbInfo);

        return dataSourceCache.computeIfAbsent(key, k -> {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(getDriverClassName(dbInfo.getDbType()));
            config.setJdbcUrl(makeJdbcUrl(dbInfo));
            config.setUsername(dbInfo.getUsername());
            config.setPassword(dbInfo.getPassword());

            // ✅ 커넥션 풀 제한 설정
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            config.setIdleTimeout(300_000); // 5분
            config.setMaxLifetime(900_000); // 15분
            config.setConnectionTimeout(30_000); // 30초

            config.setPoolName("DBMON-Pool-" + dbInfo.getDbName());
            return new HikariDataSource(config);
        });
    }

    private static String makeKey(DbConnectionInfo db) {
        return db.getDbType().toUpperCase() + ":" + db.getHost() + ":" + db.getPort() + "/" + db.getDbName();
    }

    public static String makeJdbcUrl(DbConnectionInfo d) {
        String dbType = d.getDbType().toUpperCase();
        String dbName = d.getDbName();

        switch (dbType) {
            case "ORACLE":
                if ("KIFRS".equalsIgnoreCase(dbName)) {
                    return "jdbc:oracle:thin:@//" + d.getHost() + ":" + d.getPort() + "/" + dbName;
                } else {
                    return "jdbc:oracle:thin:@" + d.getHost() + ":" + d.getPort() + ":" + dbName;
                }
            case "MYSQL":
                return "jdbc:mysql://" + d.getHost() + ":" + d.getPort() + "/" + dbName;
            case "MARIADB":
                return "jdbc:mariadb://" + d.getHost() + ":" + d.getPort() + "/" + dbName;
            case "MSSQL":
                return "jdbc:sqlserver://" + d.getHost() + ":" + d.getPort() + ";databaseName=" + dbName + ";encrypt=false";
            case "POSTGRESQL":
                return "jdbc:postgresql://" + d.getHost() + ":" + d.getPort() + "/" + dbName;
            case "TIBERO":
                return "jdbc:tibero:thin:@" + d.getHost() + ":" + d.getPort() + ":" + dbName;
            default:
                throw new IllegalArgumentException("지원하지 않는 DB 타입: " + dbType);
        }
    }

    public static String getDriverClassName(String dbType) {
        switch (dbType.toUpperCase()) {
            case "ORACLE": return "oracle.jdbc.OracleDriver";
            case "MYSQL": return "com.mysql.cj.jdbc.Driver";
            case "MARIADB": return "org.mariadb.jdbc.Driver";
            case "MSSQL": return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "POSTGRESQL": return "org.postgresql.Driver";
            case "TIBERO": return "com.tmax.tibero.jdbc.TbDriver";
            default: throw new IllegalArgumentException("지원하지 않는 DB 드라이버: " + dbType);
        }
    }
}
