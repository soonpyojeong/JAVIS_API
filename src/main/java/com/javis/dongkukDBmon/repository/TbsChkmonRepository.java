package com.javis.dongkukDBmon.repository;
import com.javis.dongkukDBmon.model.TbsChkmon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TbsChkmonRepository extends JpaRepository<TbsChkmon, Long> {
}


