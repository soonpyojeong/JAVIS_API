package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class TablespaceUsageDto {
    private String dbType;
    private String dbDescript;  // 추가된 필드
    private String dbName;
    private String tsName;
    private Long increaseMb;
}