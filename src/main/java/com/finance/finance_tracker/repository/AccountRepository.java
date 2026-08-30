package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Account save(Account account);

    Optional<Account> findById(Long accountId);

    boolean existsByNameAndUserId(String name, Long userId);

    boolean existsByNameAndUserIdAndIdNot(String name, Long userId, Long id);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    List<Account> findByUserId(Long userId);

    boolean existsByBankCodeAndExternalAccountNumber(String bankCode, String externalAccountNumber);

}
