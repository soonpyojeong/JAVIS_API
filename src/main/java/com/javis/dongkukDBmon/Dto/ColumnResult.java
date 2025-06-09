package com.javis.dongkukDBmon.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ColumnResult {
    private String sourceColumn;
    private String targetColumn;
    private String sourceType;
    private String targetType;
    private String status;
    private String warning;
}