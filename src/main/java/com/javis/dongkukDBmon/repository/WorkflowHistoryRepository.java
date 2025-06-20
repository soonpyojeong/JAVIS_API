package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {
    List<WorkflowHistory> findByWorkflowIdOrderByChangedAtDesc(Long workflowId);
}