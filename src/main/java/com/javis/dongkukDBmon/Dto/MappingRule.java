package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class MappingRule {
    private String sourceColumn;
    private String targetColumn;
    private String transform;     // ex: to_number, to_date
    private String defaultValue;  // 값 없을 때 기본값
    private String targetType;    // "NUMBER(10)", "VARCHAR2(100)", etc
}