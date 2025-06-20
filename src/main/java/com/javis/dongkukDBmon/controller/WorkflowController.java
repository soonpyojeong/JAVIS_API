package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.WorkflowHistory;
import com.javis.dongkukDBmon.model.Workflow;
import com.javis.dongkukDBmon.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    @Autowired
    private WorkflowService workflowService;

    // 등록/수정 (JSON으로 노드/엣지 전체 전달)
    @PostMapping("/save")
    public ResponseEntity<Workflow> save(@RequestBody Workflow dto, @RequestParam String user) {
        Workflow saved = workflowService.saveOrUpdate(dto, user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> get(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id); // 서비스에서 실제 삭제 처리
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<Workflow>> list() {
        return ResponseEntity.ok(workflowService.getAll());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WorkflowHistory>> history(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getHistory(id));
    }
}
