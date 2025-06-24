package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.Dto.EtlSchLogDto;
import com.javis.dongkukDBmon.model.TbEtlSchLog;
import com.javis.dongkukDBmon.repository.EtlSchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class EtlSchLogService {
    private final EtlSchLogRepository SchLogRepo;


    // Oracle 11g용 페이징
    public Page<EtlSchLogDto> findByScheduleId(Long scheduleId, int page, int size) {
        int startRow = page * size;
        int endRow = (page + 1) * size;
        List<TbEtlSchLog> logs = SchLogRepo.findLogsWithPaging(scheduleId, startRow, endRow);
        long total = SchLogRepo.countLogsByScheduleId(scheduleId);
        List<EtlSchLogDto> dtos = logs.stream().map(EtlSchLogDto::from).collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(page, size), total);
    }
}
