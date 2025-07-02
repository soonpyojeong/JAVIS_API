package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.TbRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRoleRepository extends JpaRepository<TbRole, Long> {
    // 추가 쿼리 필요시 메소드 정의
}
