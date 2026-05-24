package ru.mephi.identity.usecase.auth;

import ru.mephi.identity.domain.auth.RefreshTokenEntity;
import ru.mephi.identity.repository.auth.RefreshTokenRepository;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.config.IdentitySecurityProperties;
import ru.mephi.identity.security.RefreshTokenGenerator;
import ru.mephi.identity.security.TokenHasher;
import ru.mephi.identity.domain.user.UserEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenIssuer {

    private final RefreshTokenGenerator generator;
    private final TokenHasher hasher;
    private final RefreshTokenRepository repository;
    private final IdentitySecurityProperties properties;

    public RefreshTokenIssuer(
        RefreshTokenGenerator generator,
        TokenHasher hasher,
        RefreshTokenRepository repository,
        IdentitySecurityProperties properties
    ) {
        this.generator = generator;
        this.hasher = hasher;
        this.repository = repository;
        this.properties = properties;
    }

    public IssuedRefreshToken issue(UserEntity user, WebRequestMetadata metadata) {
        var value = generator.generate();
        var expiresAt = Instant.now().plus(properties.getRefreshTokenTtl());
        var entity = repository.save(new RefreshTokenEntity(
            UUID.randomUUID(),
            user,
            hasher.sha256(value),
            expiresAt,
            metadata.ipAddress(),
            metadata.userAgent()
        ));
        return new IssuedRefreshToken(entity.getId(), value, expiresAt);
    }

    public record IssuedRefreshToken(UUID id, String value, Instant expiresAt) {
    }
}
