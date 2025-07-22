package com.javis.dongkukDBmon.Dto;


import lombok.Data;

@Data
public class AlertSummaryDto {
    private String alertType; // 'INVALID', 'LOG', 'TBS'
    private String time;
    private String dbDesc;
    private String message;
    private String lvl;
    private String collectedAt;
    private String chkType;
}
