package com.javis.dongkukDBmon.Dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class EtlScheduleDto {
    private Long scheduleId;
    private String scheduleName;
    private List<Long> jobIds;
    private String scheduleType;
    private String scheduleExpr;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date endDate;
    private String enabledYn;
    private Date createdAt;
    private Date updatedAt;
    private String updatedUser;

    private List<String> jobs; // ← 연결된 JOB 이름 리스트


}