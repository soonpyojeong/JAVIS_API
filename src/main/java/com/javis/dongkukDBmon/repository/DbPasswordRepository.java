package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.DbPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DbPasswordRepository extends JpaRepository<DbPassword, Long> {
    List<DbPassword> findByDbNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String dbName, String username);

}
