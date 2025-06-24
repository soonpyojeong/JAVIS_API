package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.Dto.EtlScheduleDto;
import com.javis.dongkukDBmon.model.EtlSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtlScheduleRepository extends JpaRepository<EtlSchedule, Long> {
    List<EtlSchedule> findByEnabledYn(String enabledYn);
}
