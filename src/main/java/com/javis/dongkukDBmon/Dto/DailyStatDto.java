package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class DailyStatDto {
    private String chkDate;
    private String dbType;
    private String dbDescript;
    private String dbName;
    private Integer activeSession;
    private Integer dailyArchCnt;
}