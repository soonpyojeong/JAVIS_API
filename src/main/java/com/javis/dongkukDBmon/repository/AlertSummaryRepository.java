package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.Dto.AlertSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AlertSummaryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<AlertSummaryDto> getAlertSummary() {
        String sql =
                "SELECT " +
                        "ALERT_TYPE\n" +
                        ",TIME\n" +
                        ",DB_DESC\n" +
                        ",MESSAGE\n" +
                        ",LVL\n" +
                        ",CHK_TYPE " +
                        "from VW_DASHBOAD_ALERT";



        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AlertSummaryDto dto = new AlertSummaryDto();
            dto.setAlertType(rs.getString("ALERT_TYPE"));
            dto.setTime(rs.getString("TIME"));
            dto.setDbDesc(rs.getString("DB_DESC"));
            dto.setMessage(rs.getString("MESSAGE"));
            dto.setLvl(rs.getString("LVL"));
            dto.setChkType(rs.getString("CHK_TYPE"));
            return dto;
        });
    }
}