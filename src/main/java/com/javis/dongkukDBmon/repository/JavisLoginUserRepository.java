package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.JavisLoginUser;
import com.javis.dongkukDBmon.model.UserAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JavisLoginUserRepository extends JpaRepository<JavisLoginUser, Long> {
    Optional<JavisLoginUser> findByLoginId(String loginId);
    Optional<JavisLoginUser> findByEmail(String email);

    interface UserAlertRepository extends JpaRepository<UserAlert, Long> {
        List<UserAlert> findByUserIdAndIsDeletedOrderByAlert_CreatedAtDesc(String userId, String isDeleted);
        Optional<UserAlert> findByUserIdAndAlertId(String userId, Long alertId);
    }
}