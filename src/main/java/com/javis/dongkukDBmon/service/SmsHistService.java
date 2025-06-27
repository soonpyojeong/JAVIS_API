package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.SmsHistDto;
import com.javis.dongkukDBmon.model.SmsHist;
import com.javis.dongkukDBmon.repository.SmsHistRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class SmsHistService {

    private static final Logger logger = LoggerFactory.getLogger(SmsHistService.class);

    @Autowired
    private SmsHistRepository smsHistRepository;

    @PersistenceContext
    private EntityManager em;

    public List<SmsHistDto> getSmsHistories(int day) {
        String sql =
                "SELECT SEQNO, INDATE, INTIME, MEMBER, SENDID, SENDNAME, RPHONE1, RPHONE2, RPHONE3, RECVNAME, " +
                        "SPHONE1, SPHONE2, SPHONE3, MSG, URL, RDATE, RTIME, RESULT, KIND, ERRCODE, SRC " +
                        "FROM ( " +
                        "  SELECT SEQNO, INDATE, INTIME, MEMBER, SENDID, SENDNAME, RPHONE1, RPHONE2, RPHONE3, RECVNAME, " +
                        "         SPHONE1, SPHONE2, SPHONE3, MSG, URL, RDATE, RTIME, RESULT, KIND, ERRCODE, 'SMSDATA' AS SRC " +
                        "    FROM SMSDATA " +
                        "   WHERE INDATE >= TO_CHAR(TRUNC(SYSDATE) - :day + 1, 'YYYYMMDD') " +
                        "  UNION ALL " +
                        "  SELECT SEQNO, INDATE, INTIME, MEMBER, SENDID, SENDNAME, RPHONE1, RPHONE2, RPHONE3, RECVNAME, " +
                        "         SPHONE1, SPHONE2, SPHONE3, MSG, URL, RDATE, RTIME, RESULT, KIND, ERRCODE, 'HISTORY' AS SRC " +
                        "    FROM SMSDATA_HISTORY " +
                        "   WHERE INDATE >= TO_CHAR(TRUNC(SYSDATE) - :day + 1, 'YYYYMMDD') " +
                        ") " +
                        "ORDER BY INDATE DESC, INTIME DESC";

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("day", day)
                .getResultList();

        List<SmsHistDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            int i = 0;
            SmsHistDto dto = new SmsHistDto();
            dto.setSeqno(toLong(row[i++]));
            dto.setInDate(toString(row[i++]));
            dto.setInTime(toString(row[i++]));
            dto.setMember(toLong(row[i++]));
            dto.setSendId(toString(row[i++]));
            dto.setSendName(toString(row[i++]));
            dto.setRphone1(toString(row[i++]));
            dto.setRphone2(toString(row[i++]));
            dto.setRphone3(toString(row[i++]));
            dto.setRecName(toString(row[i++]));
            dto.setSphone1(toString(row[i++]));
            dto.setSphone2(toString(row[i++]));
            dto.setSphone3(toString(row[i++]));
            dto.setMsg(toString(row[i++]));
            dto.setUrl(toString(row[i++]));
            dto.setRdate(toString(row[i++]));
            dto.setRtime(toString(row[i++]));
            // ★ char(1), varchar2(1) => String 변환 (핵심!)
            dto.setResult(toString(row[i++]));
            dto.setKind(toString(row[i++]));
            // ★ NUMBER 컬럼
            dto.setErrCode(toLong(row[i++]));
            // src
            dto.setSrc(toString(row[i++]));
            result.add(dto);
        }
        return result;
    }

    // 도우미 함수들
    private String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }
    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return null;
        }
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
