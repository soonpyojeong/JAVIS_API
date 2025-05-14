package com.javis.dongkukDBmon.service;

import com.javis.dongkukDBmon.model.DbPassword;
import com.javis.dongkukDBmon.repository.DbPasswordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DbPasswordService {

    private final DbPasswordRepository repository;

    public List<DbPassword> getAll() {
        return repository.findAll();
    }

    public DbPassword save(DbPassword pass) {
        Date now = new Date();
        pass.setCreateDate(now);
        pass.setLastUpdateDate(now);
        return repository.save(pass);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<DbPassword> findById(Long id) {
        return repository.findById(id);
    }
}
