package com.javis.dongkukDBmon.mapper;

import com.javis.dongkukDBmon.Dto.MonitorModuleDto;
import com.javis.dongkukDBmon.Dto.MonitorModuleQueryDto;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.model.MonitorModuleQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonitorModuleMapper {

    public static MonitorModuleDto toDto(MonitorModule entity) {
        MonitorModuleDto dto = new MonitorModuleDto();
        dto.setId(entity.getModuleId());
        dto.setLabel(entity.getModuleName());
        dto.setModuleCode(entity.getModuleCode());
        dto.setUseYn(entity.getUseYn());
        dto.setColor(entity.getColor());
        dto.setRemark(entity.getRemark());

        // queryList DTO 변환
        List<MonitorModuleQueryDto> queryDtos = entity.getQueries().stream().map(q -> {
            MonitorModuleQueryDto qdto = new MonitorModuleQueryDto();
            qdto.setModuleQueryId(q.getModuleQueryId());
            qdto.setDbType(q.getDbType());
            qdto.setQueryText(q.getQueryText());
            qdto.setRemark(q.getRemark());
            return qdto;
        }).collect(Collectors.toList());
        dto.setQueryList(queryDtos);

        // queries Map<String, String> 생성 (DB타입 → 쿼리)
        Map<String, String> queryMap = new HashMap<>();
        for (MonitorModuleQuery q : entity.getQueries()) {
            queryMap.put(q.getDbType(), q.getQueryText());
        }
        dto.setQueries(queryMap);

        return dto;
    }
}
