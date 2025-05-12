package com.javis.dongkukDBmon.repository;
import com.javis.dongkukDBmon.model.Threshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {
    Threshold findByDbNameAndTablespaceName(String dbName, String tablespaceName);
    @Modifying
    @Query("UPDATE Threshold t SET t.imsiDel = NULL WHERE t.imsiDel IS NOT NULL AND t.imsiDel < :expireDate")
    int restoreExpiredImsiDel(@Param("expireDate") Date expireDate);

}
