package ru.mephi.identity.security;

import ru.mephi.identity.common.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

public final class PrincipalIds {

    private PrincipalIds() {
    }

    public static UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_TOKEN_REQUIRED", "A user token is required");
        }
    }

    public static String subject(Jwt jwt) {
        return jwt.getSubject();
    }
}
