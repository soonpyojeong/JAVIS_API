package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class EtlExecuteRequest {
    private Long jobId;
    private String srcTable;
    private String tgtTable;
    private String extractQuery;
    private String mappingJson;
}