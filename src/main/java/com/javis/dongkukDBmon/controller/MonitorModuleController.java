package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.MonitorModuleDto;
import com.javis.dongkukDBmon.model.MonitorModule;
import com.javis.dongkukDBmon.model.MonitorModuleQuery;
import com.javis.dongkukDBmon.service.MonitorModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/monitor-module")
@RequiredArgsConstructor
public class MonitorModuleController {
    private final MonitorModuleService service;

    @GetMapping
    public List<MonitorModuleDto> listModules() { // 엔티티 반환 X, DTO 반환 O
        return service.getAllModuleDtos();
    }


    @PostMapping
    public ResponseEntity<?> createModule(@RequestBody MonitorModuleDto monitorModuleDto) {
        log.info("받은 DTO: {}", monitorModuleDto);
        MonitorModule module = new MonitorModule();
        module.setModuleName(monitorModuleDto.getModuleName()); // ← 꼭 세팅
        service.createModule(monitorModuleDto);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModule(@PathVariable Long id, @RequestBody MonitorModuleDto dto) {
        service.updateModule(id, dto); // 서비스에 수정 로직 추가
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public void deleteModule(@PathVariable Long id) {
        service.deleteModule(id);
    }

    // 쿼리 추가
    @PostMapping("/{moduleId}/query")
    public MonitorModuleQuery addQuery(@PathVariable Long moduleId, @RequestBody MonitorModuleQuery dto) {
        return service.createQuery(moduleId, dto.getDbType(), dto.getQueryText(), dto.getRemark());
    }

    // 모듈별 쿼리 리스트
    @GetMapping("/{moduleId}/query")
    public List<MonitorModuleQuery> getQueries(@PathVariable Long moduleId) {
        return service.getQueriesByModule(moduleId);
    }

    @GetMapping("/with-queries")
    public List<MonitorModuleDto> listModulesWithQueries() {
        return service.getAllModuleDtos();
    }
}
