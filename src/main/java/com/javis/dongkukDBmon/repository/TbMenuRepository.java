package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.TbMenu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbMenuRepository extends JpaRepository<TbMenu, Long> {
    // 추가 쿼리 필요시 메소드 정의
}
