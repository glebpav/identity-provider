package ru.mephi.identity.domain.user;

import ru.mephi.identity.common.error.ApiException;
import org.springframework.http.HttpStatus;

public enum UserRole {
    TRADER,
    POSITIONER,
    ADMIN,
    AUDITOR;

    public static final UserRole DEFAULT = AUDITOR;

    public static UserRole fromDatabaseValue(String value) {
        try {
            return UserRole.valueOf(value);
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "UNKNOWN_USER_ROLE", "Unknown user role: " + value);
        }
    }
}
