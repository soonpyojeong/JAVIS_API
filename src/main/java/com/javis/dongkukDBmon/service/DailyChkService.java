package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.DBList;
import com.javis.dongkukDBmon.model.Dailychk;
import com.javis.dongkukDBmon.model.TbDailychk;
import com.javis.dongkukDBmon.repository.DBListRepository;
import com.javis.dongkukDBmon.repository.DailyChkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DailyChkService {

    @Autowired
    private DBListRepository dbListRepository;

    @Autowired
    private DailyChkRepository dailyChkRepository;

    // DBType이 ORACLE 또는 TIBERO이고 dailyChk가 'Y'인 DB 목록을 dbType별로 분리하여 반환
    public Map<String, Map<String, List<String>>> getDbListForDailyChkByType() {
        List<DBList> dbList = dbListRepository.findAll();

        return dbList.stream()
                .filter(db -> "Y".equals(db.getDailyChk()) &&
                        ("ORACLE".equals(db.getDbType()) || "TIBERO".equals(db.getDbType())))
                .collect(Collectors.groupingBy(
                        DBList::getLoc, // 1차: 지역(loc) 기준 그룹핑
                        Collectors.groupingBy(
                                DBList::getDbType, // 2차: dbType 기준 그룹핑
                                Collectors.collectingAndThen(
                                        Collectors.mapping(
                                                DBList::getInstanceName,
                                                Collectors.toCollection(LinkedHashSet::new) // 중복 제거 + 순서 유지
                                        ),
                                        set -> new ArrayList<>(set) // ✅ 여기를 Function으로 감싸야 오류 없음
                                )
                        )
                ));
    }


    // DB 이름에 따른 두 날의 DB 데이터를 조회하는 메소드
    public List<Dailychk> getDailyChkData(String instanceName) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return dailyChkRepository.findById_DbNameAndId_ChkDateBetween(instanceName);
    }
    // DB 이름에 따른 두 날의 DB 데이터를 조회하는 메소드
    public List<TbDailychk> getTbDailyChkData(String instanceName) {
        return dailyChkRepository.findById_TbDbNameAndId_ChkDateBetween(instanceName);
    }

}
