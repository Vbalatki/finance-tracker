package com.finance.finance_tracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;


@Data
@Entity
@Table(name = "roles", schema = "finance_tracker")
@NoArgsConstructor
@AllArgsConstructor
public class Role implements GrantedAuthority {
    @Id
    // Было GenerationType.AUTO: на MySQL это молча резолвилось в обычный
    // auto-increment, но на PostgreSQL Hibernate выбирает для AUTO
    // SEQUENCE-стратегию и ищет отдельный объект-последовательность в БД,
    // которого Liquibase не создавал — приложение стартовало бы нормально,
    // но падало бы именно в момент создания первой роли. IDENTITY работает
    // одинаково предсказуемо на обеих СУБД и соответствует остальным сущностям.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(nullable = false, unique = true)
    String name;

    @Override
    public String getAuthority() {
        return name;
    }
}
