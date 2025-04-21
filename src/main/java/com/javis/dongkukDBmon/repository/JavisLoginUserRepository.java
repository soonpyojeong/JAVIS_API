package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.JavisLoginUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JavisLoginUserRepository extends JpaRepository<JavisLoginUser, Long> {
    Optional<JavisLoginUser> findByLoginId(String loginId);
    Optional<JavisLoginUser> findByEmail(String email);
}