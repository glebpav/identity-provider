package ru.mephi.identity.security;

import ru.mephi.identity.config.IdentitySecurityProperties;
import ru.mephi.identity.domain.user.UserEntity;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final IdentitySecurityProperties properties;

    public AccessTokenService(JwtEncoder jwtEncoder, IdentitySecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public IssuedAccessToken issueUserToken(UserEntity user) {
        var now = Instant.now();
        var expiresAt = now.plus(properties.getAccessTokenTtl());
        var claims = JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("token_type", "user")
            .claim("user_id", user.getId().toString())
            .claim("email", user.getEmail())
            .claim("first_name", user.getFirstName())
            .claim("last_name", user.getLastName())
            .claim("role", user.getRole().name())
            .build();
        return new IssuedAccessToken(jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue(), expiresAt);
    }

    public IssuedAccessToken issueClientToken(String serviceName, String clientId, Set<String> scopes) {
        var now = Instant.now();
        var expiresAt = now.plus(properties.getAccessTokenTtl());
        var claims = JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .subject(clientId)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("token_type", "service")
            .claim("service", serviceName)
            .claim("client_id", clientId)
            .claim("scope", scopes.stream().sorted().toList())
            .build();
        return new IssuedAccessToken(jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue(), expiresAt);
    }
}
