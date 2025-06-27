package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.SmsdataHistory;
import com.javis.dongkukDBmon.service.SmsdataHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smshist")
@RequiredArgsConstructor
public class SmsdataHistoryController {

    private final SmsdataHistoryService smsdataHistoryService;

    @GetMapping("/all")
    public List<SmsdataHistory> getAllHistories(@RequestParam(defaultValue = "3") int day) {
        return smsdataHistoryService.getRecentHistories(day);
    }

    // 추가 검색, 페이징, 결과 Map 등 필요하면 바로 추가 가능!
}
