package com.javis.dongkukDBmon.Dto;

import com.javis.dongkukDBmon.model.MonitorModuleQuery;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MonitorModuleDto {
    private Long id;
    private String label;
    private String moduleName;
    private String color;
    private String moduleCode;
    private String useYn;
    private String remark;

    private Map<String, String> queries;
    private List<MonitorModuleQueryDto> queryList;  // ✅ 이 부분이 DTO 타입이어야 함
}
