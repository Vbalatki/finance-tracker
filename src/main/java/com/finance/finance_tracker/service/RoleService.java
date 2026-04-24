package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.RoleDto;

import java.util.List;

public interface RoleService {
    List<RoleDto> findAll();
    RoleDto create(RoleDto dto);
    void delete(Long id);
    RoleDto findById(Long id);
    void update(Long id, RoleDto dto);
}
