package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findAllByOrderByIdAsc();
    Optional<Role> findById(Long Id);
    Optional<Role> findByName(String roleName);
}
