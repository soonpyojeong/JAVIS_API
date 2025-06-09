package com.javis.dongkukDBmon.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class JobStatusMessage {
    private Long jobId;
    private String result;
    private Date lastRunAt;
}