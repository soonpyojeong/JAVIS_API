package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.DBList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DBListRepository extends JpaRepository<DBList, Long> {
    // DBList 엔티티에서 필요한 CRUD 기능을 제공
    @Modifying
    @Transactional
    @Query("UPDATE DBList d SET d.allChk = ?1")
    void updateAllChkStatusBulk(String status);

}

