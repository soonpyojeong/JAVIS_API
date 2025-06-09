package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EtlJobHistoryRepository extends JpaRepository<EtlJobHistory, Long> {
    List<EtlJobHistory> findByJobId(Long jobId); // 특정 Job 실행 이력 조회 등
}