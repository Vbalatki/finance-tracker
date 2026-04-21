package com.finance.finance_tracker.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit", schema = "finance_tracker")
@RequiredArgsConstructor
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;          // CREATE, UPDATE, DELETE, LOGIN, LOGOUT

    @Column(nullable = false)
    private String entityType;      // Transaction, Account, Category, Budget, User

    private Long entityId;          // ID изменённой сущности

    @Column(length = 1024)
    private String details;         // Детали изменения (JSON или текст)

    @Column(nullable = false)
    private Long userId;            // ID пользователя

    @Column(nullable = false)
    private String username;        // Имя пользователя (для быстрого поиска)

    @CreationTimestamp
    private LocalDateTime createdAt;
}
