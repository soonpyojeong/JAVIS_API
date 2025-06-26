package com.javis.dongkukDBmon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javis.dongkukDBmon.model.EtlSchedule;
import com.javis.dongkukDBmon.Dto.EtlScheduleDto;
import com.javis.dongkukDBmon.repository.EtlJobRepository;
import com.javis.dongkukDBmon.repository.EtlScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EtlScheduleService {

    private final EtlScheduleRepository schrepo;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private EtlJobRepository etlJobRepo;




    // 등록
    @Transactional
    public EtlScheduleDto save(EtlScheduleDto dto) throws JsonProcessingException {
        EtlSchedule entity = new EtlSchedule();
        entity.setScheduleName(dto.getScheduleName());
        entity.setScheduleType(dto.getScheduleType());
        entity.setScheduleExpr(dto.getScheduleExpr());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setEnabledYn(dto.getEnabledYn());
        ObjectMapper mapper = new ObjectMapper();
        entity.setJobIds(mapper.writeValueAsString(dto.getJobIds()));
        schrepo.save(entity); // ① 먼저 저장
        EtlScheduleDto savedDto = toDto(entity); // ② 저장된 엔티티 → DTO 변환
        return savedDto;
    }

    // 수정
    @Transactional
    public EtlScheduleDto update(Long id, EtlScheduleDto dto) throws JsonProcessingException {
        EtlSchedule entity = schrepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        copyDtoToEntity(dto, entity);
        entity.setScheduleId(id);
        schrepo.save(entity); // ① 먼저 저장
        EtlScheduleDto savedDto = toDto(entity); // ② 저장된 엔티티 → DTO 변환
        return savedDto;
    }

    // 삭제
    @Transactional
    public void delete(Long id) {
        schrepo.deleteById(id); // ① DB 삭제


    }


    // 전체 조회 (DTO로 반환)
    public List<EtlScheduleDto> findAll() {
        return schrepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    // 단건 조회 (DTO로 반환)
    public EtlScheduleDto findById(Long id) {
        return schrepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    // Entity → DTO 변환
    // Entity → DTO 변환
    public EtlScheduleDto toDto(EtlSchedule entity) {
        ObjectMapper mapper = new ObjectMapper();
        EtlScheduleDto dto = new EtlScheduleDto();
        dto.setScheduleId(entity.getScheduleId());
        dto.setScheduleName(entity.getScheduleName());
        dto.setScheduleType(entity.getScheduleType());
        dto.setScheduleExpr(entity.getScheduleExpr());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setEnabledYn(entity.getEnabledYn());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedUser(entity.getUpdatedUser());

        try {
            // jobIds: JSON → List<Long>
            if (entity.getJobIds() != null && !entity.getJobIds().isEmpty()) {
                List<Long> jobIds = mapper.readValue(entity.getJobIds(), new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
                dto.setJobIds(jobIds);

                // 💡 JOB 이름 매핑: 순서 보장
                // 1. id → jobName 맵
                Map<Long, String> jobNameMap = etlJobRepo.findAllById(jobIds).stream()
                        .collect(Collectors.toMap(
                                job -> job.getId(),
                                job -> job.getJobName()
                        ));

                // 2. jobIds 순서대로 jobName 매핑
                List<String> jobNames = jobIds.stream()
                        .map(jobNameMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                dto.setJobs(jobNames);

            } else {
                dto.setJobIds(null);
                dto.setJobs(null);
            }
        } catch (Exception e) {
            dto.setJobIds(null);
            dto.setJobs(null);
        }
        return dto;
    }



    private void copyDtoToEntity(EtlScheduleDto dto, EtlSchedule entity) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        entity.setScheduleName(dto.getScheduleName());
        entity.setScheduleType(dto.getScheduleType());
        entity.setScheduleExpr(dto.getScheduleExpr());
        entity.setStartDate(dto.getStartDate());
        entity.setUpdatedAt(new Date());
        entity.setUpdatedUser(dto.getUpdatedUser());
        // ⭐️ endDate를 23:59:59로 보정
        if (dto.getEndDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dto.getEndDate());
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            entity.setEndDate(cal.getTime());
        } else {
            entity.setEndDate(null);
        }

        entity.setEnabledYn(dto.getEnabledYn());
        // jobIds: List<Long> → JSON String
        if (dto.getJobIds() != null) {
            entity.setJobIds(mapper.writeValueAsString(dto.getJobIds()));
        } else {
            entity.setJobIds(null);
        }
        // createdAt/updatedAt 등은 JPA @PrePersist, @PreUpdate로 처리 권장
    }


}

