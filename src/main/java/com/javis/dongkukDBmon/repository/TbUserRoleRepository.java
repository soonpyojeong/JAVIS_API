package com.javis.dongkukDBmon.repository;


import com.javis.dongkukDBmon.model.TbUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbUserRoleRepository extends JpaRepository<TbUserRole, Long> {

    // 롤ID로 매핑된 유저롤 전체 조회
    List<TbUserRole> findByRoleId(Long roleId);

    // 유저ID로 유저롤 전체 조회 (필요시)
    List<TbUserRole> findByUserId(Long userId);

    // 복합키로 조회(필요시)
    TbUserRole findByUserIdAndRoleId(Long userId, Long roleId);

    // 필요에 따라 더 추가 가능
}
