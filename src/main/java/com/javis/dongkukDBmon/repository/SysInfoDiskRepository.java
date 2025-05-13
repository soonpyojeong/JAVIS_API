package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.SysInfoDisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysInfoDiskRepository extends JpaRepository<SysInfoDisk, Long> {
    List<SysInfoDisk> findBySummaryId(Long summaryId);

}

