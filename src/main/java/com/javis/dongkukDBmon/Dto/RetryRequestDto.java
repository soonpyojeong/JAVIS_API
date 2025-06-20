package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class RetryRequestDto {
    private Long logId;
    private Long jobId;
    private Long batchId;
    private Long sourceDbId;
    private String userId;
}
