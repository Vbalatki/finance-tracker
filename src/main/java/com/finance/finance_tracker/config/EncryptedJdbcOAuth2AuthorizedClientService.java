package com.finance.finance_tracker.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Написан с нуля против публичных API OAuth2AuthorizedClient/
 * ClientRegistration/OAuth2AccessToken/OAuth2RefreshToken, а не через
 * наследование от JdbcOAuth2AuthorizedClientService — не уверен в
 * стабильности его внутренних (не публично документированных) полей
 * между версиями spring-security-oauth2-client, а тут цена ошибки —
 * незаметно сломанное шифрование токенов.
 */
@Service
public class EncryptedJdbcOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final TokenEncryptor encryptor;

    public EncryptedJdbcOAuth2AuthorizedClientService(JdbcTemplate jdbcTemplate,
                                                       ClientRegistrationRepository clientRegistrationRepository,
                                                       TokenEncryptor encryptor) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.encryptor = encryptor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
        RowMapper<OAuth2AuthorizedClient> mapper = (rs, rowNum) -> {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
            if (registration == null) return null;

            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    encryptor.decrypt(rs.getString("access_token_value")),
                    rs.getTimestamp("access_token_issued_at").toInstant(),
                    rs.getTimestamp("access_token_expires_at").toInstant(),
                    parseScopes(rs.getString("access_token_scopes")));

            String encryptedRefresh = rs.getString("refresh_token_value");
            OAuth2RefreshToken refreshToken = null;
            if (encryptedRefresh != null) {
                Timestamp issuedAt = rs.getTimestamp("refresh_token_issued_at");
                refreshToken = new OAuth2RefreshToken(
                        encryptor.decrypt(encryptedRefresh),
                        issuedAt != null ? issuedAt.toInstant() : null);
            }

            return new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
        };

        List<OAuth2AuthorizedClient> results = jdbcTemplate.query(
                "SELECT * FROM finance_tracker.oauth2_authorized_client WHERE client_registration_id = ? AND principal_name = ?",
                mapper, clientRegistrationId, principalName);

        return results.isEmpty() ? null : (T) results.get(0);
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, org.springframework.security.core.Authentication principal) {
        String registrationId = authorizedClient.getClientRegistration().getRegistrationId();
        String principalName = principal.getName();
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_tracker.oauth2_authorized_client WHERE client_registration_id = ? AND principal_name = ?",
                Integer.class, registrationId, principalName);
        boolean exists = count != null && count > 0;

        List<String> scopesList = new ArrayList<>(accessToken.getScopes() == null ? Set.of() : accessToken.getScopes());
        String scopes = scopesList.isEmpty() ? null : String.join(",", scopesList);

        if (exists) {
            jdbcTemplate.update(
                    "UPDATE finance_tracker.oauth2_authorized_client SET access_token_type = ?, access_token_value = ?, " +
                            "access_token_issued_at = ?, access_token_expires_at = ?, access_token_scopes = ?, " +
                            "refresh_token_value = ?, refresh_token_issued_at = ? " +
                            "WHERE client_registration_id = ? AND principal_name = ?",
                    accessToken.getTokenType().getValue(),
                    encryptor.encrypt(accessToken.getTokenValue()),
                    Timestamp.from(accessToken.getIssuedAt()),
                    Timestamp.from(accessToken.getExpiresAt()),
                    scopes,
                    refreshToken != null ? encryptor.encrypt(refreshToken.getTokenValue()) : null,
                    refreshToken != null && refreshToken.getIssuedAt() != null ? Timestamp.from(refreshToken.getIssuedAt()) : null,
                    registrationId, principalName);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO finance_tracker.oauth2_authorized_client " +
                            "(client_registration_id, principal_name, access_token_type, access_token_value, " +
                            "access_token_issued_at, access_token_expires_at, access_token_scopes, " +
                            "refresh_token_value, refresh_token_issued_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    registrationId, principalName,
                    accessToken.getTokenType().getValue(),
                    encryptor.encrypt(accessToken.getTokenValue()),
                    Timestamp.from(accessToken.getIssuedAt()),
                    Timestamp.from(accessToken.getExpiresAt()),
                    scopes,
                    refreshToken != null ? encryptor.encrypt(refreshToken.getTokenValue()) : null,
                    refreshToken != null && refreshToken.getIssuedAt() != null ? Timestamp.from(refreshToken.getIssuedAt()) : null);
        }
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        jdbcTemplate.update(
                "DELETE FROM finance_tracker.oauth2_authorized_client WHERE client_registration_id = ? AND principal_name = ?",
                clientRegistrationId, principalName);
    }

    private Set<String> parseScopes(String raw) {
        Set<String> scopes = new HashSet<>();
        if (raw != null && !raw.isBlank()) {
            scopes.addAll(List.of(raw.split(",")));
        }
        return scopes;
    }
}
