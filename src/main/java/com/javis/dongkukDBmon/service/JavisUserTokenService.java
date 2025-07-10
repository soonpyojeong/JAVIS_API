package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.JavisLoginUser;
import com.javis.dongkukDBmon.model.JavisUserToken;
import com.javis.dongkukDBmon.repository.JavisLoginUserRepository;
import com.javis.dongkukDBmon.repository.JavisUserTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JavisUserTokenService {

    @Autowired
    private JavisUserTokenRepository tokenRepository;
    @Autowired
    private JavisLoginUserRepository userRepo;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    /**
     * 유저 ID로 토큰 저장 또는 갱신
     */
    public void saveOrUpdateRefreshToken(Long userId, String refreshToken) {
        Optional<JavisUserToken> optionalToken = tokenRepository.findByUserId(userId);

        if (optionalToken.isPresent()) {
            // 기존 값 갱신
            JavisUserToken token = optionalToken.get();
            token.setRefreshToken(refreshToken);
            token.setCreatedAt(new Date());
            tokenRepository.save(token);
        } else {
            // 신규 생성
            JavisUserToken token = new JavisUserToken();
            token.setUserId(userId);
            token.setRefreshToken(refreshToken);
            token.setCreatedAt(new Date());
            tokenRepository.save(token);
        }
    }

    public String getRefreshToken(Long userId) {
        return tokenRepository.findByUserId(userId)
                .map(JavisUserToken::getRefreshToken)
                .orElse(null);
    }

    public void deleteByUserId(Long userId) {
        tokenRepository.findByUserId(userId)
                .ifPresent(tokenRepository::delete);
    }

    public String createResetToken(JavisLoginUser user) {
        String token = UUID.randomUUID().toString();
        Date expiresAt = new Date(System.currentTimeMillis() + 1000 * 60 * 30); // 30분
        JavisUserToken userToken = tokenRepository.findByUserId(user.getId())
                .orElse(new JavisUserToken());
        userToken.setUserId(user.getId());
        userToken.setResetToken(token);
        userToken.setResetTokenExpiresAt(expiresAt);
        userToken.setResetTokenUsed("N");
        tokenRepository.save(userToken);
        return token;
    }

    public Optional<JavisLoginUser> findByResetToken(String token) {
        Optional<JavisUserToken> tokenOpt = tokenRepository.findByResetTokenAndResetTokenUsed(token, "N");
        if (tokenOpt.isPresent()) {
            JavisUserToken ut = tokenOpt.get();
            if (ut.getResetTokenExpiresAt().after(new Date())) {
                return Optional.ofNullable(ut.getUser());
            }
        }
        return Optional.empty();
    }

    public void updatePassword(JavisLoginUser user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public void expireResetToken(String token) {
        tokenRepository.findByResetToken(token).ifPresent(ut -> {
            ut.setResetTokenUsed("Y");
            tokenRepository.save(ut);
        });
    }

}
