package com.javis.dongkukDBmon.controller;

import com.javis.dongkukDBmon.model.LiveChkMon;
import com.javis.dongkukDBmon.service.LiveChkMonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j  // Slf4j 로깅을 사용
@RestController
@RequestMapping("/api/LiveChkmon")
public class LiveChkMonController {
    private final LiveChkMonService liveChkMonService;

    @Autowired
    public LiveChkMonController(LiveChkMonService liveChkMonService){
        this.liveChkMonService=liveChkMonService;
    }


    @GetMapping("/all")
    public List<LiveChkMon>getLiveChkMon(){
        return liveChkMonService.getAllLivechkmon();
    }
}
