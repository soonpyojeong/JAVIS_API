package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.DBList;
import com.javis.dongkukDBmon.model.Dailychk;
import com.javis.dongkukDBmon.model.TbDailychk;
import com.javis.dongkukDBmon.repository.DBListRepository;
import com.javis.dongkukDBmon.repository.DailyChkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    public Map<String, List<String>> getDbListForDailyChkByType() {
        List<DBList> dbList = dbListRepository.findAll();

        // dbType별로 ORACLE, TIBERO를 필터링하고, dailyChk가 'Y'인 데이터만 반환
        Map<String, List<String>> dbTypeMap = dbList.stream()
                .filter(db -> "Y".equals(db.getDailyChk()) &&
                        ("ORACLE".equals(db.getDbType()) || "TIBERO".equals(db.getDbType())))
                .collect(Collectors.groupingBy(
                        DBList::getDbType,  // dbType별로 분리
                        Collectors.mapping(DBList::getInstanceName,
                                Collectors.toList())  // 중복 제거 없이 List로 수집
                ));

        // 중복된 DB 이름을 제거
        dbTypeMap.replaceAll((dbType, instanceNames) -> instanceNames.stream().distinct().collect(Collectors.toList()));

        // 리턴 전에 출력
        System.out.println("DB 목록 by dbType: " + dbTypeMap);

        return dbTypeMap;
    }

    // DB 이름에 따른 두 날의 DB 데이터를 조회하는 메소드
    public List<Dailychk> getDailyChkData(String instanceName) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 디버깅용 콘솔 출력 (옵션)
        System.out.println("instanceName: " + instanceName);


        return dailyChkRepository.findById_DbNameAndId_ChkDateBetween(instanceName);
    }
    // DB 이름에 따른 두 날의 DB 데이터를 조회하는 메소드
    public List<TbDailychk> getTbDailyChkData(String instanceName) {

        // 디버깅용 콘솔 출력 (옵션)
        System.out.println("instanceName: " + instanceName);


        return dailyChkRepository.findById_TbDbNameAndId_ChkDateBetween(instanceName);
    }

}
