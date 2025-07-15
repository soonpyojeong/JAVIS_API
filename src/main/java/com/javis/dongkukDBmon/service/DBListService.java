package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.DbListDto;
import com.javis.dongkukDBmon.model.DBList;
import com.javis.dongkukDBmon.repository.DBListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DBListService {

    @Autowired
    private DBListRepository dbListRepository;

    @Autowired
    private AlertService alertService;

    // DB 리스트 모두 가져오기
    public List<DBList> getAllDBList() {
        return dbListRepository.findAll();
    }

    // DB 리스트 저장
    public void saveDBList(DBList dbList) {
        dbListRepository.save(dbList);

    }
    public List<DbListDto> getOracleAndTiberoDbList() {
        return dbListRepository.findOracleAndTiberoDbList();
    }

    // DB 리스트 삭제
    public void deleteDBList(Long id) {
        dbListRepository.deleteById(id);
    }

    // DB ID로 리스트 가져오기
    public DBList getDBListById(Long id) {
        return dbListRepository.findById(id).orElse(null);
    }

    // 전체관제 상태 업데이트 (bulk update)
    public void updateAllChkStatus(String newStatus) {
        // newStatus가 null이면 ALLCHK_N을 빈 문자열로, 그렇지 않으면 "N"으로 업데이트
        String statusToUpdate = (newStatus == null) ? "" : "N";
        dbListRepository.updateAllChkStatusBulk(statusToUpdate);
    }
}
