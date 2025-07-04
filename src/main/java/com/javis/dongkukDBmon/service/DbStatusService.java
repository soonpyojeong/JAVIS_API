package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.DbHealthStatusDto;
import com.javis.dongkukDBmon.repository.DbStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                        String.valueOf(row.get("error"))
                ))
                .toList();
    }
}
