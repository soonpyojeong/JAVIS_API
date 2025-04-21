package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.JavisUserToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JavisUserTokenRepository extends JpaRepository<JavisUserToken, Long> {
    JavisUserToken findByUserId(Long userId);
}
