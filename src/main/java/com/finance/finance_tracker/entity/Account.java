package com.finance.finance_tracker.entity;

import com.finance.finance_tracker.entity.enums.Currency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.finance.finance_tracker.util.DataConstants.LENGTH_255;

@Getter
@Setter
@Entity
@Table(name = "accounts", schema = "finance_tracker")
// check = COUNT обязателен: без него Hibernate не проверяет, сколько строк
// реально обновил этот кастомный SQL, и условие "AND version = ?" по факту
// ничего не защищает — конфликт по устаревшей версии молча проигнорируется
// вместо ObjectOptimisticLockingFailureException (см. AccountOptimisticLockingIT)
@SQLDelete(sql = "UPDATE finance_tracker.accounts SET active = false WHERE id = ? AND version = ?",
        check = ResultCheckStyle.COUNT)
@Where(clause = "active = true")
@AllArgsConstructor
@ToString(exclude = {"user", "transactions"})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = LENGTH_255)
    private String name;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "bank_code", length = 32)
    private String bankCode;                 // null для счетов, созданных вручную

    @Column(name = "external_account_number", length = 64)
    private String externalAccountNumber;    // null для счетов, созданных вручную

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Account() {
        this.balance = BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
