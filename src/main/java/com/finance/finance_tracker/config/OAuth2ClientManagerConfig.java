package com.finance.finance_tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * AuthorizedClientServiceOAuth2AuthorizedClientManager — вариант менеджера
 * БЕЗ привязки к текущему HttpServletRequest/Response (в отличие от
 * DefaultOAuth2AuthorizedClientManager). Нужен именно этот, потому что
 * BankImportServiceImpl.syncTransactions — обычный сервисный метод, не
 * вызывается напрямую из тела контроллера с доступом к request/response.
 * Провайдер-цепочка включает refresh — авто-обновление access_token по
 * refresh_token, когда истёк срок, без участия пользователя.
 */
@Configuration
public class OAuth2ClientManagerConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }
}
