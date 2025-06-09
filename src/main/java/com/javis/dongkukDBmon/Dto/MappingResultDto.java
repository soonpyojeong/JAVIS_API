package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class MappingResultDto {
    private String sourceColumn;
    private String sourceType;
    private String targetColumn;
    private String targetType;
    private String status;   // OK, 타입불일치, 누락 등
    private String warning;
}