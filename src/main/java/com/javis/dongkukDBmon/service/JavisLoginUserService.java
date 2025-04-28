package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.JavisLoginUser;
import com.javis.dongkukDBmon.model.JavisUserToken;
import com.javis.dongkukDBmon.repository.JavisLoginUserRepository;
import com.javis.dongkukDBmon.repository.JavisUserTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
}
