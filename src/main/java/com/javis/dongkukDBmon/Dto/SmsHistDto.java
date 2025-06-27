package com.javis.dongkukDBmon.Dto;

import lombok.Data;

@Data
public class SmsHistDto {
    private Long seqno;
    private String inDate;
    private String inTime;
    private Long member;
    private String sendId;
    private String sendName;
    private String rphone1;
    private String rphone2;
    private String rphone3;
    private String recName;
    private String sphone1;
    private String sphone2;
    private String sphone3;
    private String msg;
    private String url;
    private String rdate;
    private String rtime;
    private String result;
    private String kind;
    private Long errCode;
    private String src; // SMSDATA or HISTORY
}