package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.DBList;
import com.javis.dongkukDBmon.service.DBListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db-list")
public class DBListController {

    @Autowired
    private DBListService dbListService;

    @GetMapping("/all")
    public List<DBList> getAllDBList() {
        return dbListService.getAllDBList();
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveDBList(@RequestBody DBList dbList) {
        dbListService.saveDBList(dbList);
        return ResponseEntity.ok("DB List saved successfully");
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDBList(@PathVariable Long id, @RequestBody DBList dbList) {
        DBList existingDbList = dbListService.getDBListById(id);
        if (existingDbList != null) {
            existingDbList.setLiveChk(dbList.getLiveChk());
            existingDbList.setSizeChk(dbList.getSizeChk());
            existingDbList.setTrnBakChk(dbList.getTrnBakChk());
            existingDbList.setObjSegSizeChk(dbList.getObjSegSizeChk());
            existingDbList.setDailyChk(dbList.getDailyChk());

            dbListService.saveDBList(existingDbList);
            return ResponseEntity.ok("DB List updated successfully");
        }
        return ResponseEntity.status(404).body("DB List not found");
    }

    // GET 엔드포인트 추가: 전체관제 상태 조회 (/allchk)
    @GetMapping("/allchk")
    public ResponseEntity<String> getAllChkStatus() {
        // 여기서는 첫번째 DB 항목의 ALLCHK_N 값을 반환하는 예시입니다.
        List<DBList> list = dbListService.getAllDBList();
        String status = (list != null && !list.isEmpty()) ? list.get(0).getAllChk() : null;
        return ResponseEntity.ok(status);
    }

    // PUT 엔드포인트 수정: 요청 본문을 JSON 객체로 받아서 처리
    @PutMapping("/update-allchk")
    public ResponseEntity<?> updateAllChk(@RequestBody Map<String, String> payload) {
        String status = payload.get("status");  // "N" 또는 null
        dbListService.updateAllChkStatus(status);
        return ResponseEntity.ok("전체관제 상태 업데이트 완료");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDBList(@PathVariable Long id) {
        dbListService.deleteDBList(id);
        return ResponseEntity.ok("DB List deleted successfully");
    }
}
