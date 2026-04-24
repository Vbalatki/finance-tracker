package com.finance.finance_tracker.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.CascadeType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.finance.finance_tracker.Util.DataConstants.LENGTH_255;

@Entity
@Table(name = "users", schema = "finance_tracker")
@SQLDelete(sql = "UPDATE users SET active = false WHERE id = ?")        // При delete вызывается UPDATE
@Where(clause = "active = true")                                        // Автоматически добавляется WHERE ко всем запросам
@FilterDef(name = "activeFilter", defaultCondition = "active = true")   //
@Filter(name = "activeFilter")                                          // Для enable/disable фильтра
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = LENGTH_255)
    private String name;

    @Column(name = "surname", nullable = false, length = LENGTH_255)
    private String surname;

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "email", unique = true, nullable = false, length = LENGTH_255)
    private String email;

    @Column(name = "password", nullable = false, length = LENGTH_255)
    private String password;

    @Column(name = "active", nullable = false) // для мягкого удаления сущностей
    private boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
