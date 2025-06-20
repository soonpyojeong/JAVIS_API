package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.MonitorModuleQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorModuleQueryRepository extends JpaRepository<MonitorModuleQuery, Long> {
    List<MonitorModuleQuery> findByModule_ModuleId(Long moduleId);
    List<MonitorModuleQuery> findByDbType(String dbType);
}
