package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.SmsdataHistory;
import com.javis.dongkukDBmon.repository.SmsdataHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SmsdataHistoryService {

    private final SmsdataHistoryRepository smsdataHistoryRepository;

    public List<SmsdataHistory> getRecentHistories(int day) {
        return smsdataHistoryRepository.findRecentHistories(day);
    }
}
