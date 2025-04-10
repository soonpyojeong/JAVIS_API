package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.SmsHist;
import com.javis.dongkukDBmon.repository.SmsHistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class SmsHistService {

    private static final Logger logger = LoggerFactory.getLogger(SmsHistService.class);

    @Autowired
    private SmsHistRepository smsHistRepository;

    public List<SmsHist> getSmsHistories(int day) {
        // day 값을 이용하여 'day'일 수만큼 이전 날짜 이후의 데이터 조회
        logger.info("Fetching SMS histories for the last {} days", day);

        // 쿼리 실행 전 로그
        logger.debug("Calling findSmsHistoriesAfterDays with day={}", day);

        List<SmsHist> histories = smsHistRepository.findSmsHistoriesAfterDays(day);

        // 쿼리 실행 후 로그
        logger.debug("Found {} SMS histories", histories.size());

        return histories;
    }
    public int updateAllSmsHistories() {
        int updatedCount = 0;
        List<SmsHist> histories = smsHistRepository.findAll();
        for (SmsHist history : histories) {
            if ("0".equals(history.getResult())) {
                history.setResult("1");
                smsHistRepository.save(history);
                updatedCount++;
            }
        }
        return updatedCount;
    }

}
