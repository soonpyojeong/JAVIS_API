package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.MonitorModuleDto;
import com.javis.dongkukDBmon.Dto.MonitorModuleQueryDto;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.model.MonitorModuleQuery;
import com.javis.dongkukDBmon.repository.MonitorModuleQueryRepository;
import com.javis.dongkukDBmon.repository.MonitorModuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorModuleService {
    private final MonitorModuleRepository moduleRepo;
    private final MonitorModuleQueryRepository queryRepo;



    public MonitorModule getMonitorModuleWithQueries(Long id) {
        return moduleRepo.findByIdWithQueries(id)
                .orElseThrow(() -> new IllegalArgumentException("No MonitorModule: " + id));
    }

    // 엔티티 → DTO 변환
    public List<MonitorModuleDto> getAllModuleDtos() {
        return moduleRepo.findAll().stream().map(module -> {
            MonitorModuleDto dto = new MonitorModuleDto();
            dto.setId(module.getModuleId());
            dto.setLabel(module.getModuleName());
            dto.setColor(module.getColor());
            dto.setModuleCode(module.getModuleCode());
            dto.setUseYn(module.getUseYn());
            dto.setRemark(module.getRemark());

            // 1. DB타입별 쿼리 (Map)
            Map<String, String> queriesMap = module.getQueries().stream()
                    .collect(Collectors.toMap(MonitorModuleQuery::getDbType, MonitorModuleQuery::getQueryText));
            dto.setQueries(queriesMap);

            // 2. 쿼리 전체 리스트 (DTO 변환)
            List<MonitorModuleQueryDto> queryDtoList = module.getQueries().stream().map(q -> {
                MonitorModuleQueryDto qdto = new MonitorModuleQueryDto();
                qdto.setModuleQueryId(q.getModuleQueryId());
                qdto.setDbType(q.getDbType());
                qdto.setQueryText(q.getQueryText());
                qdto.setRemark(q.getRemark());
                return qdto;
            }).collect(Collectors.toList());
            dto.setQueryList(queryDtoList);

            return dto;
        }).collect(Collectors.toList());
    }

    // 모듈 신규 등록
    public void createModule(MonitorModuleDto dto) {
        MonitorModule module = new MonitorModule();
        module.setModuleName(dto.getLabel());
        module.setModuleCode(dto.getModuleCode());
        module.setColor(dto.getColor());
        module.setRemark(dto.getRemark());
        module.setUseYn("Y");

        // queryList 변환 및 부모 연결!
        List<MonitorModuleQuery> queries = dto.getQueryList().stream().map(qDto -> {
            MonitorModuleQuery q = new MonitorModuleQuery();
            q.setDbType(qDto.getDbType());
            q.setQueryText(qDto.getQueryText());
            q.setRemark(qDto.getRemark());
            q.setModule(module); // 부모 연결 중요!!
            return q;
        }).collect(Collectors.toList());
        module.setQueries(queries);

        moduleRepo.save(module); // Cascade = ALL 이면 같이 저장됨
    }
    // 모듈 전체 조회
    public List<MonitorModule> getAllModules() {
        return moduleRepo.findAll();
    }
    @Transactional
    public void updateModule(Long id, MonitorModuleDto dto) {
        MonitorModule module = moduleRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found"));

        // 기존 queries를 안전하게 clear하고, 새로운 쿼리 엔티티 추가
        module.getQueries().clear();
        for (MonitorModuleQueryDto qDto : dto.getQueryList()) {
            MonitorModuleQuery q = new MonitorModuleQuery();
            q.setDbType(qDto.getDbType());
            q.setQueryText(qDto.getQueryText());
            q.setRemark(qDto.getRemark());
            q.setModule(module);
            module.getQueries().add(q);
        }

        module.setModuleName(dto.getLabel());
        module.setModuleCode(dto.getModuleCode());
        module.setColor(dto.getColor());
        module.setRemark(dto.getRemark());

        moduleRepo.save(module);
    }



    // 모듈 삭제
    public void deleteModule(Long moduleId) {
        moduleRepo.deleteById(moduleId);
    }

    // DB타입별 쿼리 등록
    public MonitorModuleQuery createQuery(Long moduleId, String dbType, String queryText, String remark) {
        MonitorModule module = moduleRepo.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 모듈"));
        MonitorModuleQuery query = new MonitorModuleQuery();
        query.setModule(module);
        query.setDbType(dbType);
        query.setQueryText(queryText);
        query.setRemark(remark);
        return queryRepo.save(query);
    }

    // 모듈별 쿼리 조회
    public List<MonitorModuleQuery> getQueriesByModule(Long moduleId) {
        return queryRepo.findByModule_ModuleId(moduleId);
    }
}
