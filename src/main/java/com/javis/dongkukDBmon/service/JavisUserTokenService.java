package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.JavisUserToken;
import com.javis.dongkukDBmon.repository.JavisUserTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class JavisUserTokenService {

    @Autowired
    private JavisUserTokenRepository tokenRepository;

    /**
     * 유저 ID로 토큰 저장 또는 갱신
     */
    public void saveOrUpdateRefreshToken(Long userId, String refreshToken) {
        Optional<JavisUserToken> optionalToken = Optional.ofNullable(tokenRepository.findByUserId(userId));

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
        JavisUserToken token = tokenRepository.findByUserId(userId);
        return token != null ? token.getRefreshToken() : null;
    }
    public void deleteByUserId(Long userId) {
        JavisUserToken token = tokenRepository.findByUserId(userId);
        if (token != null) {
            tokenRepository.delete(token);
        }
    }
}
