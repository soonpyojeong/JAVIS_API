package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.DbHealthStatusDto;
import com.javis.dongkukDBmon.repository.DbStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.sql.Clob;
import java.io.Reader;
import java.io.BufferedReader;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DbStatusService {

    private final DbStatusRepository repository;

    public List<DbHealthStatusDto> fetchLiveStatuses() {
        return repository.fetchDbHealthStatusRaw().stream()
                .map(row -> new DbHealthStatusDto(
                        String.valueOf(row.get("name")),
                        String.valueOf(row.get("status")),
                        String.valueOf(row.get("chkDate")),
                        String.valueOf(row.get("message")),
                        clobToString(row.get("error"))  // ✅ 핵심 수정
                ))
                .toList();
    }

    private String clobToString(Object clobObj) {
        if (clobObj == null) return null;
        if (clobObj instanceof String str) return str;
        if (clobObj instanceof Clob clob) {
            StringBuilder sb = new StringBuilder();
            try (Reader reader = clob.getCharacterStream();
                 BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Exception e) {
                return "[CLOB 변환 실패] " + e.getMessage();
            }
            return sb.toString().trim();
        }
        return clobObj.toString(); // fallback
    }
}
