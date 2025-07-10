package com.javis.dongkukDBmon.Dto;


import lombok.Data;

@Data
public class DefaultThresholdUpdateRequest {
    private Long defThresMb;
    private String commt; // ← username 대신 commt 이름 그대로 사용
    // getter/setter
}