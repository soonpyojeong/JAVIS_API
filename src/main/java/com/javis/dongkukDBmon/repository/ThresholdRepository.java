package com.javis.dongkukDBmon.repository;
import com.javis.dongkukDBmon.model.Threshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {
    Threshold findByDbNameAndTablespaceName(String dbName, String tablespaceName);
}
