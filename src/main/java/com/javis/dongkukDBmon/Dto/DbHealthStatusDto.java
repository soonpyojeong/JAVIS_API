package com.javis.dongkukDBmon.Dto;

public record DbHealthStatusDto(
        String name,         // 인스턴스명
        String status,       // 상태: 정상, 주의, 위험, 미수집
        String chkDate,
        String message,
        String error,
        String dbType
) {}
