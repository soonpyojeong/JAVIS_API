package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class DbConnectionInfoDto {
    private Long id;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String description; // 선택사항
    private Long dbid; // TB_JAVIS_DB_LIST의 ID
}
