package com.finance.finance_tracker.config;

import com.finance.finance_tracker.util.SecurityUtil;
import com.finance.finance_tracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Язык берётся из настроек аутентифицированного пользователя (User.locale),
 * а не из cookie/сессии — смена языка одним пользователем не влияет на
 * других и переживает вход с другого устройства. Для неаутентифицированных
 * запросов (страница логина) — откат на обычный SessionLocaleResolver.
 *
 * Имя бина "localeResolver" ОБЯЗАТЕЛЬНО — DispatcherServlet ищет его по
 * этому конкретному имени, не по типу.
 */
@Component("localeResolver")
@RequiredArgsConstructor
public class UserAwareLocaleResolver implements LocaleResolver {

    private final UserService userService;
    private final SessionLocaleResolver fallbackResolver = new SessionLocaleResolver();

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId != null) {
            try {
                String locale = userService.getUserSettings(userId).getLocale();
                return Locale.forLanguageTag(locale);
            } catch (Exception e) {
                // не должны ронять страницу из-за проблем с настройками
            }
        }
        return fallbackResolver.resolveLocale(request);
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        fallbackResolver.setLocale(request, response, locale);
    }
}