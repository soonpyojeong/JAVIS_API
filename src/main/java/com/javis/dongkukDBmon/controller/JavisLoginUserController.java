package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.JavisLoginUser;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String loginId = loginData.get("loginId"); // 클라이언트와 동일한 필드 이름 사용
        String password = loginData.get("password");

        if (loginId == null || loginId.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "ID 또는 비밀번호가 누락되었습니다."
            ));
        }

        Optional<JavisLoginUser> user = userService.login(loginId, password);
        if (user.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("user", user.get());
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
}
