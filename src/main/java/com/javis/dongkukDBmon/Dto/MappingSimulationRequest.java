package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class MappingSimulationRequest {
    private String extractQuery;
    private String mappingJson;
    private Integer sampleCount;
    private Long sourceDbId;
    private Long targetDbId;
    private String targetTable;
}