package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class TsSummaryRequestDto {
    private String dbName;         // 필수
    private String filterType;     // month | single | range
    private String startDate;      // yyyyMMdd
    private String endDate;        // yyyyMMdd (단일 날짜일 경우에도 동일하게 세팅)
}