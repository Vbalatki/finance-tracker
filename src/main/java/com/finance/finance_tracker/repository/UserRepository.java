package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //create
    User save(User user);

    //Поиск
    Optional<User> findById(Long id);

    @Query("SELECT u FROM User u JOIN u.accounts a WHERE a.id = :accountId")
    Optional<User> findByAccountId(Long accountId);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    //delete
    //void delete(User user);

    //exists
    boolean existsByEmail(String email);
}
