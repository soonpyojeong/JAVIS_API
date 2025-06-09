package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EtlJobRepository extends JpaRepository<EtlJob, Long> {
    // 커스텀 메서드 예시 (상태별 조회 등)
    List<EtlJob> findByStatus(String status);

}