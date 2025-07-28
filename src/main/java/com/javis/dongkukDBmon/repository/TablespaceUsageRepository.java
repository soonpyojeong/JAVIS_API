package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.Dto.TablespaceUsageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TablespaceUsageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<TablespaceUsageDto> getMonthlyUsage() {
        String sql = "SELECT DB_TYPE, DB_DESCRIPT, DB_NAME, TS_NAME, MONTHLY_INC_MB FROM TB_MONTHLY_INC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TablespaceUsageDto dto = new TablespaceUsageDto();
            dto.setDbType(rs.getString("DB_TYPE"));
            dto.setDbDescript(rs.getString("DB_DESCRIPT")); // 추가됨
            dto.setDbName(rs.getString("DB_NAME"));
            dto.setTsName(rs.getString("TS_NAME"));
            dto.setIncreaseMb(rs.getLong("MONTHLY_INC_MB"));
            return dto;
        });
    }

    public List<TablespaceUsageDto> getWeeklyUsage() {
        String sql = "SELECT DB_TYPE, DB_DESCRIPT, DB_NAME, TS_NAME, WEEKLY_INC_MB FROM TB_WEEKLY_INC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TablespaceUsageDto dto = new TablespaceUsageDto();
            dto.setDbType(rs.getString("DB_TYPE"));
            dto.setDbDescript(rs.getString("DB_DESCRIPT")); // 추가됨
            dto.setDbName(rs.getString("DB_NAME"));
            dto.setTsName(rs.getString("TS_NAME"));
            dto.setIncreaseMb(rs.getLong("WEEKLY_INC_MB"));
            return dto;
        });
    }

}
