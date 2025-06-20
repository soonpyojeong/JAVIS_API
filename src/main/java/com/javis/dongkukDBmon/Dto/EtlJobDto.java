package com.javis.dongkukDBmon.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class EtlJobDto {
    private Long id;
    private String jobName;
    private List<Long> sourceDbIds;
    private Long monitorModuleId;
    private Long targetDbId;
    private String extractQuery;
    private Map<String, String> extractQueries;
    private String targetTable;
    private String schedule;
    private String status;
    private String sourceDbName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private String lastResult;

    private List<DbConnectionInfoDto> sourceDbs;  // optional, for 조회
    private DbConnectionInfoDto targetDb;

    private String monitorModuleLabel; // optional, for 조회
}
