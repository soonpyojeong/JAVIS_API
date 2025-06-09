package com.javis.dongkukDBmon.Dto;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class MappingSimulationResult {
    private Map<String, Object> mappedRow;
    private List<ColumnResult> columns;

    // ⭐️ 모든 필드 받는 생성자 직접 추가!
    public MappingSimulationResult(Map<String, Object> mappedRow, List<ColumnResult> columns) {
        this.mappedRow = mappedRow;
        this.columns = columns;
    }

    @Data
    public static class ColumnResult {
        private String sourceColumn;
        private String targetColumn;
        private Object value;
        private String valueType;
        private String warning;

        // ⭐️ 모든 필드 받는 생성자 직접 추가!
        public ColumnResult(String sourceColumn, String targetColumn, Object value, String valueType, String warning) {
            this.sourceColumn = sourceColumn;
            this.targetColumn = targetColumn;
            this.value = value;
            this.valueType = valueType;
            this.warning = warning;
        }
    }
}