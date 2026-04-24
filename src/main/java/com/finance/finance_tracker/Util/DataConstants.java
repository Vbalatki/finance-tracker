package com.finance.finance_tracker.Util;

import org.springframework.stereotype.Component;

@Component
public class DataConstants {

    public static final int LENGTH_255 = 255;
    public static final String USER_NOT_FOUND = "Пользователь не найден";
    public static final String CATEGORY_NOT_FOUND = "Категория не найдена";
    public static final String BUDGET_NOT_FOUND = "Бюджет не найден";
    public static final String INVALID_MONTHLY_LIMIT = "Месячный лимит должен быть положительным числом";
    public static final String AUDIT_NOT_FOUND = "Запись аудита не найдена";
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String ACCOUNT_NOT_FOUND = "Счёт не найден";
    public static final String ACCOUNT_NAME_EXISTS = "Счёт с таким именем уже существует";
    public static final String INSUFFICIENT_FUNDS = "Недостаточно средств на счёте";
    public static final String INVALID_AMOUNT = "Сумма должна быть положительной";
    public static final String CATEGORY_NAME_EXISTS = "Категория с таким именем уже существует";
    public static final String CANNOT_DELETE_CATEGORY = "Невозможно удалить категорию, так как есть связанные транзакции";
    public static final String CATEGORY_NAME_BLANK = "Название категории не может быть пустым";
    public static final String ROLE_NOT_FOUND = "Роль не найдена";
    public static final String ROLE_ALREADY_EXISTS = "Роль уже существует";
    public static final String CANNOT_MODIFY_DEFAULT_ROLE = "Нельзя изменять стандартные роли";
    public static final String CANNOT_DELETE_DEFAULT_ROLE = "Нельзя удалять стандартные роли";
    public static final String ROLE_NAME_BLANK = "Название роли не может быть пустым";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String DEFAULT_ROLE_ADMIN = "ROLE_ADMIN";
    public static final String DEFAULT_ROLE_USER = "ROLE_USER";
    public static final String TRANSACTION_NOT_FOUND = "Транзакция не найдена";
    public static final String ACCOUNT_ID_REQUIRED = "ID счёта обязателен";
    public static final String EMAIL_ALREADY_EXISTS = "Пользователь с таким email уже зарегистрирован";
    public static final String ACCOUNT_NAME_NOT_UNIQUE = "Имя счёта должно быть уникальным для пользователя";
    public static final String INCORRECT_CURRENT_PASSWORD = "Текущий пароль введён неверно";
    public static final String PASSWORD_TOO_SHORT = "Пароль должен содержать минимум 8 символов";
    public static final String MIN_PASSWORD_LENGTH = "8";
}
