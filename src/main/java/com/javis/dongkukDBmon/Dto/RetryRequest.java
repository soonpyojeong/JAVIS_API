package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class RetryRequest {
    private Long sourceDbId;
    private Long logId;
    private String triggeredBy;
}
