package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.MonitorModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MonitorModuleRepository extends JpaRepository<MonitorModule, Long> {
    Optional<MonitorModule> findByModuleCode(String moduleCode);
    @Query("SELECT m FROM MonitorModule m LEFT JOIN FETCH m.queries WHERE m.moduleId = :id")
    Optional<MonitorModule> findByIdWithQueries(@Param("id") Long id);

}
