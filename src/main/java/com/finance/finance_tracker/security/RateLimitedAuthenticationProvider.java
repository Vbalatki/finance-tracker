package com.finance.finance_tracker.security;

import com.finance.finance_tracker.service.impl.LoginAttemptService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class RateLimitedAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider delegate;
    private final LoginAttemptService loginAttemptService;

    public RateLimitedAuthenticationProvider(AuthenticationProvider delegate, LoginAttemptService loginAttemptService) {
        this.delegate = delegate;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();

        if (loginAttemptService.isBlocked(username)) {
            throw new LockedException("Слишком много неудачных попыток входа. Попробуйте позже.");
        }

        try {
            Authentication result = delegate.authenticate(authentication);
            loginAttemptService.clearAttempts(username);
            return result;
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailedAttempt(username);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}