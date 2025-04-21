package com.javis.dongkukDBmon.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long ACCESS_TOKEN_VALIDITY = 1000 * 30; // 30초
    //private final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 15; // 15분
    private final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7; // 7일

    public String generateAccessToken(String loginId) {
        return Jwts.builder()
                .setSubject(loginId)
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String loginId) {
        return Jwts.builder()
                .setSubject(loginId)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getLoginIdFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
