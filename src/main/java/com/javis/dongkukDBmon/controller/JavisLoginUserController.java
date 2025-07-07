package com.javis.dongkukDBmon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javis.dongkukDBmon.Dto.MenuAuthDto;
import com.javis.dongkukDBmon.Dto.UserRoleUpdateDto;
import com.javis.dongkukDBmon.config.JwtTokenProvider;
import com.javis.dongkukDBmon.model.*;
import com.javis.dongkukDBmon.repository.*;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import com.javis.dongkukDBmon.service.JavisUserTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@RestController
@RequestMapping("/api/auth")
public class JavisLoginUserController {

    @Autowired
    private JavisLoginUserService userService;
    @Autowired
    private JavisLoginUserRepository userRepo;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JavisUserTokenService userTokenService;
    @Autowired
    private TbRoleRepository roleRepo;
    @Autowired
    private TbMenuRepository menuRepo;
    @Autowired
    private TbRoleMenuRepository roleMenuRepo;
    @Autowired
    private TbUserRoleRepository userRoleRepo;

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
            List<MenuAuthDto> menuAuthList = userService.getUserMenuAuthList(user.getId());
            try {
                ObjectMapper mapper = new ObjectMapper();
                log.info("✅ [{}] menuAuthList: {}", user.getLoginId(), mapper.writeValueAsString(menuAuthList));
            } catch (Exception e) {
                log.warn("MenuAuthDto JSON 변환 실패", e);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("user", user);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("menuAuthList", menuAuthList);

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

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        Optional<JavisLoginUser> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "해당 사용자를 찾을 수 없습니다."
            ));
        }

        // ❗️삭제 수행 필요
        userService.deleteUser(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "사용자가 삭제되었습니다."
        ));
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

    @GetMapping("/role/all")
    public List<TbRole> getAllRoles() { return roleRepo.findAll(); }

    @GetMapping("/menu/all")
    public List<TbMenu> getAllMenus() { return menuRepo.findAll(); }

    @GetMapping("/role/{roleId}/users")
    public List<JavisLoginUser> findUsersByRole(@PathVariable Long roleId) { // @PathVariable 추가!
        List<TbUserRole> userRoles = userRoleRepo.findByRoleId(roleId);
        List<Long> userIds = userRoles.stream().map(TbUserRole::getUserId).toList();
        return userRepo.findAllById(userIds); // 여기로 변경!
    }

    @GetMapping("/role-menu/auth")
    public ResponseEntity<?> getRoleMenuAuth(@RequestParam Long roleId, @RequestParam Long menuId) {
        Optional<TbRoleMenu> auth = roleMenuRepo.findByRoleIdAndMenuId(roleId, menuId);
        return ResponseEntity.ok(auth.orElseGet(() -> {
            TbRoleMenu empty = new TbRoleMenu();
            empty.setRoleId(roleId); empty.setMenuId(menuId);
            empty.setCanRead("N"); empty.setCanWrite("N"); empty.setCanDelete("N");
            return empty;
        }));
    }

    @PostMapping("/role-menu/save")
    public ResponseEntity<?> saveRoleMenuAuth(@RequestBody TbRoleMenu dto) {
        Optional<TbRoleMenu> old = roleMenuRepo.findByRoleIdAndMenuId(dto.getRoleId(), dto.getMenuId());
        TbRoleMenu saved;
        if (old.isPresent()) {
            TbRoleMenu entity = old.get();
            entity.setCanRead(dto.getCanRead());
            entity.setCanWrite(dto.getCanWrite());
            entity.setCanDelete(dto.getCanDelete());
            saved = roleMenuRepo.save(entity);
        } else {
            saved = roleMenuRepo.save(dto);
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/role-menu/list")
    public ResponseEntity<?> getRoleMenuList(@RequestParam(required = false) Long roleId) {
        // roleId 있으면 필터, 없으면 전체
        if (roleId != null)
            return ResponseEntity.ok(roleMenuRepo.findByRoleId(roleId));
        return ResponseEntity.ok(roleMenuRepo.findAll());
    }

    @PostMapping("/user-role/update")
    public ResponseEntity<?> updateUserRole(@RequestBody UserRoleUpdateDto dto) {
        userService.updateUserRole(dto.getUserId(), dto.getUserRole(), dto.getRoleId());
        return ResponseEntity.ok().build();
    }






}
