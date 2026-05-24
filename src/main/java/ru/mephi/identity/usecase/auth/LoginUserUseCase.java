package ru.mephi.identity.usecase.auth;

import ru.mephi.identity.service.audit.AuditService;
import ru.mephi.identity.api.auth.LoginRequest;
import ru.mephi.identity.api.auth.TokenResponse;
import ru.mephi.identity.common.error.ApiException;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.config.IdentitySecurityProperties;
import ru.mephi.identity.security.AccessTokenService;
import ru.mephi.identity.domain.user.UserStatus;
import ru.mephi.identity.repository.user.UserRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final IdentitySecurityProperties properties;
    private final AuditService auditService;

    public LoginUserUseCase(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        AccessTokenService accessTokenService,
        RefreshTokenIssuer refreshTokenIssuer,
        IdentitySecurityProperties properties,
        AuditService auditService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional
    public TokenResponse execute(LoginRequest request, WebRequestMetadata metadata) {
        var now = Instant.now();
        var user = users.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> {
                auditService.record(null, "UNKNOWN", "LOGIN_FAILED", metadata.ipAddress(), null);
                return badCredentials();
            });

        if (!user.isLoginAllowed(now)) {
            auditService.record(user.getId().toString(), "USER", "LOGIN_BLOCKED", metadata.ipAddress(), "status=" + user.getStatus());
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_NOT_ACTIVE", "User cannot login");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user, now, metadata);
            throw badCredentials();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }

        var accessToken = accessTokenService.issueUserToken(user);
        var refreshToken = refreshTokenIssuer.issue(user, metadata);
        auditService.record(user.getId().toString(), "USER", "LOGIN_SUCCEEDED", metadata.ipAddress(), null);
        return new TokenResponse("Bearer", accessToken.value(), accessToken.expiresAt(), refreshToken.value(), refreshToken.expiresAt());
    }

    private void registerFailedAttempt(ru.mephi.identity.domain.user.UserEntity user, Instant now, WebRequestMetadata metadata) {
        var attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= properties.getMaxFailedLoginAttempts()) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(now.plus(properties.getLockDuration()));
        }
        auditService.record(user.getId().toString(), "USER", "LOGIN_FAILED", metadata.ipAddress(), "attempts=" + attempts);
    }

    private ApiException badCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid credentials");
    }
}
