package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.MenuAuthDto;
import com.javis.dongkukDBmon.model.JavisLoginUser;
import com.javis.dongkukDBmon.model.JavisUserToken;
import com.javis.dongkukDBmon.model.TbUserRole;
import com.javis.dongkukDBmon.repository.JavisLoginUserRepository;
import com.javis.dongkukDBmon.repository.TbUserRoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JavisLoginUserService {

    @Autowired
    private JavisLoginUserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TbUserRoleRepository userRoleRepo;

    // 회원가입 로직
    public JavisLoginUser register(JavisLoginUser newUser) throws Exception {
        if (userRepository.findByLoginId(newUser.getLoginId()).isPresent()) {
            throw new Exception("이미 존재하는 로그인 ID입니다.");
        }

        // 비밀번호 암호화
        String rawPassword = newUser.getPassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // 회원가입 시 암호화된 비밀번호를 출력
        //System.out.println("회원가입 - 평문 비밀번호: " + rawPassword);
        //System.out.println("회원가입 - 암호화된 비밀번호: " + hashedPassword);

        newUser.setPassword(hashedPassword);
        return userRepository.save(newUser);
    }

    // 로그인 로직
    public Optional<JavisLoginUser> login(String loginId, String password) {
        Optional<JavisLoginUser> user = userRepository.findByLoginId(loginId);

        if (user.isPresent()) {
            String storedHashedPassword = user.get().getPassword();

            //System.out.println("로그인 - 입력된 평문 비밀번호: " + password);
            //System.out.println("로그인 - 저장된 암호화된 비밀번호: " + storedHashedPassword);

            // 입력된 비밀번호와 저장된 암호화된 비밀번호 비교
            if (passwordEncoder.matches(password, storedHashedPassword)) {
                System.out.println("비밀번호 일치: 로그인 성공");
                return user;
            } else {
                System.out.println("비밀번호 불일치: 로그인 실패");
            }
        } else {
            System.out.println("로그인 실패: 사용자 ID가 존재하지 않습니다.");
        }

        return Optional.empty();
    }
    public Optional<JavisLoginUser> findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId);
    }
    public List<String> getAllLoginIds() {
        return userRepository.findAll()
                .stream()
                .map(user -> user.getLoginId())
                .collect(Collectors.toList());
    }

    public List<MenuAuthDto> getUserMenuAuthList(Long userId) {
        String sql = "SELECT m.MENU_PATH, "
                + "MAX(rm.CAN_READ) AS CAN_READ, "
                + "MAX(rm.CAN_WRITE) AS CAN_WRITE, "
                + "MAX(rm.CAN_DELETE) AS CAN_DELETE "
                + "FROM JAVIS_LOGIN_USER u "
                + "JOIN TB_USER_ROLE ur ON u.ID = ur.USER_ID "
                + "JOIN TB_ROLE_MENU rm ON ur.ROLE_ID = rm.ROLE_ID "
                + "JOIN TB_MENU m ON rm.MENU_ID = m.MENU_ID "
                + "WHERE u.ID = ? "
                + "GROUP BY m.MENU_PATH";

        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> {
            MenuAuthDto dto = new MenuAuthDto();
            dto.setMenuPath(rs.getString("MENU_PATH"));
            dto.setCanRead(rs.getString("CAN_READ"));
            dto.setCanWrite(rs.getString("CAN_WRITE"));
            dto.setCanDelete(rs.getString("CAN_DELETE"));
            return dto;
        });
    }

    public List<JavisLoginUser> findUsersByRole(Long roleId) {
        List<TbUserRole> userRoles = userRoleRepo.findByRoleId(roleId);
        List<Long> userIds = userRoles.stream().map(TbUserRole::getUserId).toList();
        return userRepository.findAllById(userIds); // 여기가 맞는 코드!
    }

    public void updateUserRole(Long userId, String userRole, Long roleId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId가 null입니다. 요청을 확인하세요.");
        }

        Optional<JavisLoginUser> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다. userId=" + userId);
        }

        JavisLoginUser user = optionalUser.get();
        user.setUserRole(userRole);
        userRepository.save(user);

        List<TbUserRole> mappings = userRoleRepo.findByUserId(userId);

        TbUserRole mapping;
        if (!mappings.isEmpty()) {
            mapping = mappings.get(0);
        } else {
            mapping = new TbUserRole();
            mapping.setUserId(userId);
        }

        mapping.setRoleId(roleId);
        userRoleRepo.save(mapping);
    }



}
