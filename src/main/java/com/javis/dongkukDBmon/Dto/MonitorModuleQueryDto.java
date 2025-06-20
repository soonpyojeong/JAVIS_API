package com.javis.dongkukDBmon.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitorModuleQueryDto {
    private Long moduleQueryId;
    private String dbType;
    private String queryText;
    private String remark;
}
