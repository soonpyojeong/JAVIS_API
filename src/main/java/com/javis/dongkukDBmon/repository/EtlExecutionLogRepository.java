package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtlExecutionLogRepository extends JpaRepository<EtlExecutionLog, Long> {
    // jobId로 실행 이력 조회
    List<EtlExecutionLog> findByJobIdOrderByExecDateDesc(Long jobId);
}