package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.JavisUserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JavisUserTokenRepository extends JpaRepository<JavisUserToken, Long> {

    @Query(value = "SELECT * FROM JAVIS_USER_TOKEN WHERE RESET_TOKEN = :token AND RESET_TOKEN_USED = :used", nativeQuery = true)
    Optional<JavisUserToken> findByResetTokenAndResetTokenUsed(@Param("token") String token, @Param("used") String used);

    @Query(value = "SELECT * FROM JAVIS_USER_TOKEN WHERE RESET_TOKEN = :token", nativeQuery = true)
    Optional<JavisUserToken> findByResetToken(@Param("token") String token);

    @Query(value = "SELECT * FROM JAVIS_USER_TOKEN WHERE USER_ID = :userId", nativeQuery = true)
    Optional<JavisUserToken> findByUserId(@Param("userId") Long userId);
}
