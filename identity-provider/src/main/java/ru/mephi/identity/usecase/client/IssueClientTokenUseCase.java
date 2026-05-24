package ru.mephi.identity.usecase.client;

import ru.mephi.identity.service.audit.AuditService;
import ru.mephi.identity.common.error.ApiException;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.config.IdentityServiceClientProperties;
import ru.mephi.identity.security.AccessTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IssueClientTokenUseCase {

    private final IdentityServiceClientProperties serviceClients;
    private final AccessTokenService accessTokenService;
    private final AuditService auditService;

    public IssueClientTokenUseCase(
        IdentityServiceClientProperties serviceClients,
        AccessTokenService accessTokenService,
        AuditService auditService
    ) {
        this.serviceClients = serviceClients;
        this.accessTokenService = accessTokenService;
        this.auditService = auditService;
    }

    public ClientTokenResult execute(String grantType, String clientId, String clientSecret, String scope, WebRequestMetadata metadata) {
        if (!"client_credentials".equals(grantType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_GRANT_TYPE", "Only client_credentials is supported");
        }
        var client = serviceClients.findByClientId(clientId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CLIENT_CREDENTIALS", "Invalid client credentials"));
        if (!constantTimeEquals(client.clientSecret(), clientSecret)) {
            auditService.record(clientId, "CLIENT", "CLIENT_LOGIN_FAILED", metadata.ipAddress(), null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CLIENT_CREDENTIALS", "Invalid client credentials");
        }

        var requestedScopes = parseScopes(scope);
        var allowedScopes = client.scopes();
        if (!allowedScopes.containsAll(requestedScopes)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SCOPE_NOT_ALLOWED", "Requested scope is not allowed");
        }

        var effectiveScopes = requestedScopes.isEmpty() ? allowedScopes : requestedScopes;
        var token = accessTokenService.issueClientToken(client.serviceName(), clientId, effectiveScopes);
        auditService.record(clientId, "CLIENT", "CLIENT_TOKEN_ISSUED", metadata.ipAddress(), null);
        return new ClientTokenResult("Bearer", token.value(), token.expiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond(), effectiveScopes);
    }

    private Set<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scope.trim().split("\\s+")).collect(Collectors.toSet());
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public record ClientTokenResult(String tokenType, String accessToken, long expiresIn, Set<String> scope) {
    }
}
