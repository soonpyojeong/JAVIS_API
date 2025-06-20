package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.DbConnectionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DbConnectionInfoRepository extends JpaRepository<DbConnectionInfo, Long> {
    List<DbConnectionInfo> findByDbType(String dbType);

}