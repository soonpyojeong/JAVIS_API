package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.MonitorModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonitorModuleRepository extends JpaRepository<MonitorModule, Long> {
    Optional<MonitorModule> findByModuleCode(String moduleCode);
}
