package com.javis.dongkukDBmon.service;


import com.javis.dongkukDBmon.model.TbsChkmon;
import com.javis.dongkukDBmon.repository.TbsChkmonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TbsChkmonService {
    @Autowired
    private TbsChkmonRepository tbsChkmonRepository;

    public List<TbsChkmon> getAllTbsChkmon() {
        return tbsChkmonRepository.findAll();
    }
}
