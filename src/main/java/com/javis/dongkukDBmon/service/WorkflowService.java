package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.WorkflowHistory;
import com.javis.dongkukDBmon.model.Workflow;
import com.javis.dongkukDBmon.repository.WorkflowHistoryRepository;
import com.javis.dongkukDBmon.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class WorkflowService {
    @Autowired
    private WorkflowRepository workflowRepo;
    @Autowired
    private WorkflowHistoryRepository historyRepo;

    // 저장/수정
    @Transactional
    public Workflow saveOrUpdate(Workflow dto, String user) {
        Workflow saved;
        if (dto.getWorkflowId() != null) {
            Workflow prev = workflowRepo.findById(dto.getWorkflowId()).orElseThrow();
            // 이력 저장
            WorkflowHistory hist = new WorkflowHistory();
            hist.setWorkflowId(prev.getWorkflowId());
            hist.setWorkflowJson(prev.getWorkflowJson());
            hist.setChangedBy(user);
            hist.setChangeDesc("수정");
            hist.setChangedAt(new Date());
            historyRepo.save(hist);

            // 데이터 수정
            prev.setWorkflowName(dto.getWorkflowName());
            prev.setDescription(dto.getDescription());
            prev.setWorkflowJson(dto.getWorkflowJson());
            prev.setUpdatedAt(new Date());
            prev.setUpdatedBy(user);
            saved = workflowRepo.save(prev);
        } else {
            dto.setCreatedAt(new Date());
            dto.setCreatedBy(user);
            saved = workflowRepo.save(dto);
        }
        return saved;
    }
    @Transactional
    public void deleteWorkflow(Long id) {
        workflowRepo.deleteById(id);
    }
    // 단일 조회
    public Workflow getWorkflow(Long id) {
        return workflowRepo.findById(id).orElse(null);
    }

    // 전체 조회
    public List<Workflow> getAll() {
        return workflowRepo.findAll();
    }

    // 이력 조회
    public List<WorkflowHistory> getHistory(Long workflowId) {
        return historyRepo.findByWorkflowIdOrderByChangedAtDesc(workflowId);
    }
}
