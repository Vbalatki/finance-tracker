package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Восстановление пароля — Финансовый трекер");
        message.setText("""
                Здравствуйте!

                Вы (или кто-то другой) запросили сброс пароля для этого аккаунта.

                Чтобы установить новый пароль, перейдите по ссылке (действует 30 минут):
                %s

                Если вы не запрашивали сброс — просто проигнорируйте это письмо,
                пароль останется прежним.
                """.formatted(resetLink));

        mailSender.send(message);
        log.info("Письмо сброса пароля отправлено на {}", toEmail);
    }
}