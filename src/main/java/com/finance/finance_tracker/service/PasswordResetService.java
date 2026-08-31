package com.finance.finance_tracker.service;

public interface PasswordResetService {

    /**
     * Если пользователь с таким email существует — создаёт токен и шлёт
     * письмо. Если не существует — молча ничего не делает: форма не
     * должна становиться способом проверить, кто зарегистрирован.
     */
    void requestReset(String email, String resetLinkBase);

    /**
     * @throws com.finance.finance_tracker.exception.InvalidDataException если токен не существует, использован или истёк
     */
    void validateToken(String token);

    /**
     * @throws com.finance.finance_tracker.exception.InvalidDataException если токен невалиден
     */
    void resetPassword(String token, String newPassword);
}