package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.LiveChkMon;
import com.javis.dongkukDBmon.repository.LiveChkMonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiveChkMonService {

    @Autowired
    private LiveChkMonRepository liveChkMonRepository;

    public List<LiveChkMon> getAllLivechkmon(){
        return liveChkMonRepository.findall();
    }
}
