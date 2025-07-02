package com.javis.dongkukDBmon.repository;

import com.javis.dongkukDBmon.model.TbRoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TbRoleMenuRepository extends JpaRepository<TbRoleMenu, Long> {
    Optional<TbRoleMenu> findByRoleIdAndMenuId(Long roleId, Long menuId);

    List<TbRoleMenu> findByRoleId(Long roleId);
}
