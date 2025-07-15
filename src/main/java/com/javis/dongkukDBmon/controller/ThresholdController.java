package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.DefaultThresholdUpdateRequest;
import com.javis.dongkukDBmon.Dto.ThresholdWithUsageDto;
import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.model.Threshold;
import com.javis.dongkukDBmon.service.AlertService;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import com.javis.dongkukDBmon.service.ThresholdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Slf4j
@RestController
@RequestMapping("/api/threshold")
public class ThresholdController {

    private final ThresholdService thresholdService;
    private final AlertService alertService;
    private final JavisLoginUserService javisLoginUserService;

    @Autowired
    public ThresholdController(ThresholdService thresholdService, AlertService alertService, JavisLoginUserService javisLoginUserService) {
        this.thresholdService = thresholdService;
        this.alertService = alertService;
        this.javisLoginUserService = javisLoginUserService;
    }

    @GetMapping("/all")
    public List<Threshold> getAllThresholds() {
        return thresholdService.getAllThresholds();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Boolean> updateThreshold(@PathVariable Long id, @RequestBody Map<String, Object> requestMap) {
        Optional<Threshold> optionalThreshold = thresholdService.getThresholdById(id);

        if (optionalThreshold.isPresent()) {
            Threshold threshold = optionalThreshold.get();

            // ✅ 값 꺼내기
            Integer thresMb = (Integer) requestMap.get("thresMb");
            String username = (String) requestMap.get("username");

            threshold.setThresMb(thresMb);
            thresholdService.save(threshold);

            // ✅ 알람 메시지에 username 포함
            String message = String.format("%s DB의 %s 임계치가 %d MB로  %s에 의해 수정되었습니다.",
                    threshold.getDbName(), threshold.getTablespaceName(), threshold.getThresMb(), username);

            Alert alert = alertService.createAlert("THRESHOLD_UPDATE", message);
            List<String> allUserIds = javisLoginUserService.getAllLoginIds();
            alertService.notifyUsers(alert, allUserIds);
            alertService.sendAlertToUsers(message);

            return ResponseEntity.ok(true);
        }
        return ResponseEntity.status(404).body(false);
    }


    @PutMapping("/{id}/release")
    public ResponseEntity<Boolean> releaseThreshold(@PathVariable Long id) {
        Optional<Threshold> optionalThreshold = thresholdService.getThresholdById(id);

        if (optionalThreshold.isPresent()) {
            Threshold threshold = optionalThreshold.get();

            threshold.setImsiDel(new Date()); // 현재 시간으로 임시해제
            thresholdService.save(threshold);

            // 알림 발송
            String message = String.format("%s DB의 %s 테이블스페이스 관제가 임시(3일) 해제되었습니다.",
                    threshold.getDbName(), threshold.getTablespaceName());
            Alert alert = alertService.createAlert("THRESHOLD_RELEASE", message);
            List<String> allUserIds = javisLoginUserService.getAllLoginIds();
            alertService.notifyUsers(alert, allUserIds);
            alertService.sendAlertToUsers(message);

            return ResponseEntity.ok(true);
        }

        return ResponseEntity.status(404).body(false);
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> updateDefaultThreshold(
            @PathVariable Long id,
            @RequestBody DefaultThresholdUpdateRequest dto) {

        thresholdService.updateDefaultThreshold(id, dto.getDefThresMb(), dto.getCommt());
        return ResponseEntity.ok(true);
    }


    @PostMapping("/save")
    public ResponseEntity<Threshold> saveThreshold(@RequestBody Map<String, Object> requestMap) {
        log.info("Received request to save threshold: {}", requestMap);

        // ✅ 값 꺼내기
        String dbName = (String) requestMap.get("dbName");
        String dbType = (String) requestMap.get("dbType"); // ✅ dbType 추가로 꺼내기
        String tablespaceName = (String) requestMap.get("tablespaceName");
        Integer thresMb = (Integer) requestMap.get("thresMb");
        String username = (String) requestMap.get("username");
        String chkFlag = (String) requestMap.get("chkFlag");

        // Threshold 객체 생성
        Threshold threshold = new Threshold();
        threshold.setDbName(dbName);
        threshold.setDbType(dbType);  // ✅ dbType도 꼭 세팅해야 함
        threshold.setTablespaceName(tablespaceName);
        threshold.setThresMb(thresMb);
        threshold.setDefThresMb(thresMb);
        threshold.setChkFlag(chkFlag);

        Threshold savedThreshold = thresholdService.saveThreshold(threshold);

        log.info("Threshold successfully saved: {}", savedThreshold);

        // ✅ 알람 메시지에 username 포함
        String message = String.format("%s DB의 %s 임계치가 %s 에 의해 새로 등록되었습니다. (설정 용량: %d MB)",
        savedThreshold.getDbName(), savedThreshold.getTablespaceName(), username, savedThreshold.getThresMb());

        Alert alert = alertService.createAlert("THRESHOLD_SAVE", message);
        List<String> allUserIds = javisLoginUserService.getAllLoginIds();
        alertService.notifyUsers(alert, allUserIds);
        alertService.sendAlertToUsers(message);

        return ResponseEntity.ok(savedThreshold);
    }



    @GetMapping("/with-usage")
    public ResponseEntity<List<ThresholdWithUsageDto>> getThresholdsWithUsage() {
        // 현재 시간에서 seoul기준
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // "yyyy/MM/dd HH:mm:ss" 형식으로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedTimeLimit = now.format(formatter);
        // 쿼리 실행

        List<ThresholdWithUsageDto> result = thresholdService.findThresholdsWithUsage(formattedTimeLimit);
        return ResponseEntity.ok(result);
    }


}
