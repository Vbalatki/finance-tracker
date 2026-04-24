package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.RoleDto;
import com.finance.finance_tracker.entity.Role;
import com.finance.finance_tracker.mapper.RoleMapper;
import com.finance.finance_tracker.repository.RoleRepository;
import com.finance.finance_tracker.service.RoleService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional
    public List<RoleDto> findAll() {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleDto create(RoleDto dto) {
        if (roleRepository.findByName("ROLE_" + dto.getName().toUpperCase()).isPresent()) {
            throw new RuntimeException("Role already exists");
        }
        Role role = new Role();
        role.setName("ROLE_" + dto.getName().toUpperCase());
        return roleMapper.toDto(roleRepository.save(role));
    }

    @Transactional
    public RoleDto findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("role not found with id: " + id));
        return roleMapper.toDto(role);
    }

    @Transactional
    public void update(Long id, RoleDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("role not found with id: " + id));

        if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_USER")) {
            throw new RuntimeException("Cannot modify default roles");
        }
        role.setName(dto.getName());
        roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        if (role.getName().equals("ROLE_ADMIN") || role.getName().equals("ROLE_USER")) {
            throw new RuntimeException("Cannot delete default roles");
        }
        roleRepository.delete(role);
    }
}
