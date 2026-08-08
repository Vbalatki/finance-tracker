package com.finance.finance_tracker.util;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.exception.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getId();
        }
        return null;
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    public static void requireOwnership(AccountDto account) {
        if (!account.getUserId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("Нет доступа к этому счёту");
        }
    }
}
