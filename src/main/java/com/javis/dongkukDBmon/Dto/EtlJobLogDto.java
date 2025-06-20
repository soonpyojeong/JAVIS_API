package com.javis.dongkukDBmon.Dto;

import lombok.Data;

import java.util.Date;

@Data
public class EtlJobLogDto {
    private Long logId;
    private Long jobId;         // ✅ 추가
    private Long sourceDbId;    // ✅ 추가
    private String sourceDbName;
    private String result;
    private String message;
    private Date executedAt;
}