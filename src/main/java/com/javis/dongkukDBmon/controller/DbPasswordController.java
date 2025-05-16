package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.model.DbPassword;
import com.javis.dongkukDBmon.repository.DbPasswordRepository;
import com.javis.dongkukDBmon.service.AlertService;
import com.javis.dongkukDBmon.service.DbPasswordService;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pass")
@RequiredArgsConstructor
public class DbPasswordController {

    private final DbPasswordService service;
    private final DbPasswordRepository dbPasswordRepository;
    private final AlertService alertService;
    private final JavisLoginUserService javisLoginUserService;

    @GetMapping("/list")
    public List<DbPassword> getAllPasswords() {
        return service.getAll();
    }

    @PostMapping
    public ResponseEntity<DbPassword> savePassword(@RequestBody DbPassword pass) {
        pass.setCreateDate(new Date());
        DbPassword saved = service.save(pass);

        // ✅ 알람 메시지 생성
        String message = String.format("%s / %s 계정의 패스워드가 %s에 의해 등록되었습니다.",
                saved.getDbName(), saved.getUsername(), saved.getCreateUser());

        // ✅ 알람 생성 및 전송
        Alert alert = alertService.createAlert("PASSWD_CREATE", message);
        List<String> allUserIds = javisLoginUserService.getAllLoginIds();
        alertService.notifyUsers(alert, allUserIds);
        alertService.sendAlertToUsers(message);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deletePasswordWithUser(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String username = (String) body.get("username");

        Optional<DbPassword> optional = dbPasswordRepository.findById(id);
        if (optional.isPresent()) {
            DbPassword deleted = optional.get();

            String message = String.format("%s / %s 계정의 패스워드가 %s에 의해 삭제되었습니다.",
                    deleted.getDbName(), deleted.getUsername(), username);

            service.delete(id);

            Alert alert = alertService.createAlert("PASSWD_DELETE", message);
            List<String> allUserIds = javisLoginUserService.getAllLoginIds();
            alertService.notifyUsers(alert, allUserIds);
            alertService.sendAlertToUsers(message);
        }

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/search")
    public List<DbPassword> search(@RequestBody Map<String, String> body) {
        String keyword = body.get("keyword");
        return dbPasswordRepository.findByDbNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @RequestBody DbPassword updated) {
        Optional<DbPassword> optional = dbPasswordRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DbPassword existing = optional.get();

        // 기존 필드에 업데이트된 값 반영
        existing.setGubun(updated.getGubun());
        existing.setDbType(updated.getDbType());
        existing.setDbName(updated.getDbName());
        existing.setUsername(updated.getUsername());
        existing.setPassword(updated.getPassword());
        existing.setAccStatus(updated.getAccStatus());
        existing.setExplanation(updated.getExplanation());
        existing.setManager(updated.getManager());
        existing.setIpaddr(updated.getIpaddr());
        existing.setLastUpdateUser(updated.getCreateUser());
        existing.setLastUpdateDate(new Date());

        dbPasswordRepository.save(existing);

        // ✅ 알람 메시지 생성
        String message = String.format("%s / %s 계정의 패스워드가 %s에 의해 수정되었습니다.",
                existing.getDbName(), existing.getUsername(), updated.getCreateUser());

        // ✅ 알람 생성 및 전송
        Alert alert = alertService.createAlert("PASSWD_UPDATE", message);
        List<String> allUserIds = javisLoginUserService.getAllLoginIds();
        alertService.notifyUsers(alert, allUserIds);
        alertService.sendAlertToUsers(message);

        return ResponseEntity.ok().build();
    }
}
