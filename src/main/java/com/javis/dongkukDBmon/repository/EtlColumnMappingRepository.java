package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.EtlColumnMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlColumnMappingRepository extends JpaRepository<EtlColumnMapping, Long> {
    // 필요하면 JOB_ID, SRC_TABLE, TGT_TABLE로 조회도 추가 가능
}
