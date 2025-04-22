package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.config.JwtTokenProvider;
import com.javis.dongkukDBmon.model.JavisLoginUser;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import com.javis.dongkukDBmon.service.JavisUserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class JavisLoginUserController {

    @Autowired
    private JavisLoginUserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JavisUserTokenService userTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String loginId = loginData.get("loginId");
        String password = loginData.get("password");

        if (loginId == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "ID 또는 비밀번호가 누락되었습니다."
            ));
        }

        Optional<JavisLoginUser> userOpt = userService.login(loginId, password);
        if (userOpt.isPresent()) {
            JavisLoginUser user = userOpt.get();

            // ✅ JWT 발급
            String accessToken = jwtTokenProvider.generateAccessToken(loginId);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginId);

            // ✅ 토큰 DB에 저장 (정상 분기 처리)
            try {
                userTokenService.saveOrUpdateRefreshToken(user.getId(), refreshToken);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "토큰 저장 중 오류 발생: " + e.getMessage()
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("user", user);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);

            //System.out.println("✅ 로그인 성공");
            //System.out.println("📦 accessToken: " + accessToken);
            //System.out.println("📦 refreshToken: " + refreshToken);
            //System.out.println("👤 사용자 정보: " + user);

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "로그인 실패: 잘못된 ID 또는 비밀번호입니다."
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody JavisLoginUser newUser) {
        try {
            JavisLoginUser createdUser = userService.register(newUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "회원가입 성공",
                    "user", createdUser
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> tokenData) {
        String refreshToken = tokenData.get("refreshToken");

        // 1. 토큰 누락 체크
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "refreshToken이 필요합니다."
            ));
        }

        // 2. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "유효하지 않은 refreshToken입니다."
            ));
        }

        // 3. 토큰에서 loginId 추출
        String loginId = jwtTokenProvider.getLoginIdFromToken(refreshToken);

        // 4. 사용자 조회
        Optional<JavisLoginUser> userOpt = userService.findByLoginId(loginId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "해당 사용자가 존재하지 않습니다."
            ));
        }

        JavisLoginUser user = userOpt.get();

        // 5. DB에 저장된 refreshToken과 일치 여부 확인
        String storedRefreshToken = userTokenService.getRefreshToken(user.getId());
        if (!refreshToken.equals(storedRefreshToken)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "DB에 저장된 토큰과 일치하지 않습니다."
            ));
        }

        // 6. accessToken 재발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getLoginId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "accessToken", newAccessToken
        ));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String loginId = request.get("loginId");

        // 사용자 찾기
        Optional<JavisLoginUser> userOpt = userService.findByLoginId(loginId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "사용자를 찾을 수 없습니다."
            ));
        }

        // DB에서 refreshToken 제거
        userTokenService.deleteByUserId(userOpt.get().getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "로그아웃 성공"
        ));
    }

}
