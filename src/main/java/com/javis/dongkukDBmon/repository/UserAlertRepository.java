package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.UserAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAlertRepository extends JpaRepository<UserAlert, Long> {
    List<UserAlert> findByUserIdAndIsDeletedOrderByAlert_CreatedAtDesc(String userId, String isDeleted);
    Optional<UserAlert> findByUserIdAndAlertId(String userId, Long alertId);
}