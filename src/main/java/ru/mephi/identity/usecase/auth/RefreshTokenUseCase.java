package ru.mephi.identity.usecase.auth;

import ru.mephi.identity.service.audit.AuditService;
import ru.mephi.identity.api.auth.RefreshTokenRequest;
import ru.mephi.identity.api.auth.TokenResponse;
import ru.mephi.identity.repository.auth.RefreshTokenRepository;
import ru.mephi.identity.common.error.ApiException;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.security.AccessTokenService;
import ru.mephi.identity.security.TokenHasher;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final AuditService auditService;

    public RefreshTokenUseCase(
        RefreshTokenRepository refreshTokens,
        TokenHasher tokenHasher,
        AccessTokenService accessTokenService,
        RefreshTokenIssuer refreshTokenIssuer,
        AuditService auditService
    ) {
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.accessTokenService = accessTokenService;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.auditService = auditService;
    }

    @Transactional
    public TokenResponse execute(RefreshTokenRequest request, WebRequestMetadata metadata) {
        var now = Instant.now();
        var token = refreshTokens.findByTokenHash(tokenHasher.sha256(request.refreshToken()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (token.getReplacedByTokenId() != null) {
            auditService.record(token.getUser().getId().toString(), "USER", "REFRESH_TOKEN_REUSE_DETECTED", metadata.ipAddress(), "tokenId=" + token.getId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Refresh token reuse detected");
        }
        if (!token.isActive(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is not active");
        }
        if (!token.getUser().isLoginAllowed(now)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_NOT_ACTIVE", "User cannot refresh tokens");
        }

        var replacement = refreshTokenIssuer.issue(token.getUser(), metadata);
        token.replaceWith(replacement.id(), now);
        var accessToken = accessTokenService.issueUserToken(token.getUser());
        auditService.record(token.getUser().getId().toString(), "USER", "TOKEN_REFRESHED", metadata.ipAddress(), null);
        return new TokenResponse("Bearer", accessToken.value(), accessToken.expiresAt(), replacement.value(), replacement.expiresAt());
    }
}
