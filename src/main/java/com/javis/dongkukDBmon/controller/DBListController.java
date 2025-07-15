package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.Dto.DbListDto;
import com.javis.dongkukDBmon.model.Alert;
import com.javis.dongkukDBmon.model.DBList;
import com.javis.dongkukDBmon.service.AlertService;
import com.javis.dongkukDBmon.service.DBListService;
import com.javis.dongkukDBmon.service.JavisLoginUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/db-list")
public class DBListController {

    @Autowired
    private DBListService dbListService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;
    private final JavisLoginUserService javisLoginUserService;

    @GetMapping("/all")
    public List<DBList> getAllDBList() {
        return dbListService.getAllDBList();
    }

    // ✅ Vue에서 사용할 드롭다운 리스트용 API
    @GetMapping
    public List<DbListDto> getDbListForDropdown() {
        return dbListService.getOracleAndTiberoDbList();
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveDBList(@RequestBody DBList dbList) {
        dbListService.saveDBList(dbList);
        messagingTemplate.convertAndSend("/topic/db-status", dbList);
        return ResponseEntity.ok("DB List saved successfully");
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDBList(@PathVariable Long id, @RequestBody Map<String, Object> requestMap) {
        DBList existingDbList = dbListService.getDBListById(id);

        if (existingDbList != null) {
            List<String> changes = new ArrayList<>();

            // 원래 dbList 필드들 매핑
            String liveChk = (String) requestMap.get("liveChk");
            String sizeChk = (String) requestMap.get("sizeChk");
            String trnBakChk = (String) requestMap.get("trnBakChk");
            String objSegSizeChk = (String) requestMap.get("objSegSizeChk");
            String dailyChk = (String) requestMap.get("dailyChk");
            String dbDescript = (String) requestMap.get("dbDescript");
            existingDbList.setLoc((String) requestMap.get("loc"));
            existingDbList.setAssets((String) requestMap.get("assets"));
            existingDbList.setDbDescript((String) requestMap.get("dbDescript"));
            existingDbList.setSmsGroup((String) requestMap.get("smsGroup"));
            existingDbList.setDbType((String) requestMap.get("dbType"));
            existingDbList.setDbVer((String) requestMap.get("dbVer"));
            existingDbList.setOs((String) requestMap.get("os"));
            existingDbList.setHostname((String) requestMap.get("hostname"));
            existingDbList.setDbName((String) requestMap.get("dbName"));
            existingDbList.setInstanceName((String) requestMap.get("instanceName"));
            existingDbList.setPubIp((String) requestMap.get("pubIp"));
            existingDbList.setVip((String) requestMap.get("vip"));
            existingDbList.setPort((String) requestMap.get("port"));
            existingDbList.setUserid((String) requestMap.get("userid"));
            existingDbList.setPw((String) requestMap.get("pw"));

            // ✅ 추가: userId 받아오기
            String username = (String) requestMap.get("username");

            if (!Objects.equals(existingDbList.getLiveChk(), liveChk)) {
                changes.add(String.format("LIVE: %s → %s", existingDbList.getLiveChk(), liveChk));
            }
            if (!Objects.equals(existingDbList.getSizeChk(), sizeChk)) {
                changes.add(String.format("TBS: %s → %s", existingDbList.getSizeChk(), sizeChk));
            }
            if (!Objects.equals(existingDbList.getTrnBakChk(), trnBakChk)) {
                changes.add(String.format("TRN: %s → %s", existingDbList.getTrnBakChk(), trnBakChk));
            }
            if (!Objects.equals(existingDbList.getObjSegSizeChk(), objSegSizeChk)) {
                changes.add(String.format("OBJ: %s → %s", existingDbList.getObjSegSizeChk(), objSegSizeChk));
            }
            if (!Objects.equals(existingDbList.getDailyChk(), dailyChk)) {
                changes.add(String.format("DAILY: %s → %s", existingDbList.getDailyChk(), dailyChk));
            }

            // DB 업데이트
            existingDbList.setLiveChk(liveChk);
            existingDbList.setSizeChk(sizeChk);
            existingDbList.setTrnBakChk(trnBakChk);
            existingDbList.setObjSegSizeChk(objSegSizeChk);
            existingDbList.setDailyChk(dailyChk);

            dbListService.saveDBList(existingDbList);

            // 웹소켓 전송
            messagingTemplate.convertAndSend("/topic/db-status", existingDbList);

            // 알람 생성
            if (!changes.isEmpty()) {
                String message = String.format("%s DB 상태가 변경되었습니다 by [%s]: [%s]",
                        dbDescript, username, String.join(", ", changes)); // ✅ by userId 추가
                Alert alert = alertService.createAlert("DB_STATUS", message);
                List<String> allUserIds = javisLoginUserService.getAllLoginIds();
                alertService.notifyUsers(alert, allUserIds);
                alertService.sendAlertToUsers(message);
            }

            return ResponseEntity.ok("DB List updated successfully");
        }
        return ResponseEntity.status(404).body("DB List not found");
    }


    @GetMapping("/allchk")
    public ResponseEntity<String> getAllChkStatus() {
        List<DBList> list = dbListService.getAllDBList();
        String status = (list != null && !list.isEmpty()) ? list.get(0).getAllChk() : null;
        return ResponseEntity.ok(status);
    }

    @PutMapping("/update-allchk")
    public ResponseEntity<?> updateAllChk(@RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        dbListService.updateAllChkStatus(status);

        String message = (status == null) ? "전체관제가 활성화되었습니다." : "전체관제가 중지되었습니다.";
        Alert alert = alertService.createAlert("ALLCHK_STATUS", message);
        List<String> allUserIds = javisLoginUserService.getAllLoginIds();
        alertService.notifyUsers(alert, allUserIds);
        alertService.sendAlertToUsers(message);

        return ResponseEntity.ok("전체관제 상태 업데이트 완료");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDBList(@PathVariable Long id) {
        dbListService.deleteDBList(id);
        return ResponseEntity.ok("DB List deleted successfully");
    }
}
