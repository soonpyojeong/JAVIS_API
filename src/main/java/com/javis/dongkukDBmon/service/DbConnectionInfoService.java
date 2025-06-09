package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.config.AesUtil;
import com.javis.dongkukDBmon.model.DbConnectionInfo;
import com.javis.dongkukDBmon.repository.DbConnectionInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DbConnectionInfoService {


    private final DbConnectionInfoRepository repo;

    @Value("${aes.key}")
    private String aesKey;

    // 모든 목록(복호화해서 반환 - 주의: UI에 패스워드 평문 노출 X, 커넥션 할 때만 복호화 추천)

    public List<DbConnectionInfo> getAll() {
        List<DbConnectionInfo> list = repo.findAll();
        // 패스워드는 화면에 굳이 보여줄 필요 없으면 null 처리
        list.forEach(info -> info.setPassword(null));
        return list;
    }

    // 상세 조회(커넥션 테스트 등 패스워드 평문 필요시 복호화)

    public DbConnectionInfo get(Long id) {
        DbConnectionInfo info = repo.findById(id).orElse(null);
        if (info != null) {
            try {
                info.setPassword(AesUtil.decrypt(aesKey, info.getPassword()));
            } catch (Exception e) {
                throw new RuntimeException("패스워드 복호화 오류", e);
            }
        }
        return info;
    }

    // 저장/수정 (패스워드는 반드시 암호화해서 저장)

    public DbConnectionInfo save(DbConnectionInfo d) {
        System.out.println("[DEBUG] save() 진입, pw=" + d.getPassword());

        try {
            String before = d.getPassword();
            // 임시로 무조건 암호화!
            String enc = AesUtil.encrypt(aesKey, before);
            System.out.println("[DEBUG] 암호화 전: " + before);
            System.out.println("[DEBUG] 암호화 후: " + enc);
            d.setPassword(enc);
        } catch (Exception e) {
            System.out.println("[ERROR] 암호화 예외: " + e.getMessage());
            e.printStackTrace();
        }
        return repo.save(d);
    }



    public void delete(Long id) { repo.deleteById(id); }


    public List<DbConnectionInfo> getByType(String type) {
        List<DbConnectionInfo> list = repo.findByDbType(type);
        list.forEach(info -> info.setPassword(null));
        return list;
    }

    // 암호문 체크 (간단 버전, 필요시 Base64 및 길이 등 체크)
    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
