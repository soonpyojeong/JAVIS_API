package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    // 기본 CRUD
}
