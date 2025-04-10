package com.javis.dongkukDBmon.controller;
import com.javis.dongkukDBmon.model.TbsChkmon;
import com.javis.dongkukDBmon.service.TbsChkmonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j  // Slf4j 로깅을 사용
@RestController
@RequestMapping("/api/tbsChkmon")
public class TbsChkmonController {
    private final TbsChkmonService tbsChkmonService;

    @Autowired
    public TbsChkmonController(TbsChkmonService tbsChkmonService) {
        this.tbsChkmonService = tbsChkmonService;
    }


    @GetMapping("/all")
    public List<TbsChkmon> getTbsChkmon() {
        return tbsChkmonService.getAllTbsChkmon(); // 서비스 메서드에서 데이터를 반환
    }

}
