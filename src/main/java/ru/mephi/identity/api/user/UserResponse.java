package ru.mephi.identity.api.user;

import ru.mephi.identity.domain.user.UserEntity;
import ru.mephi.identity.domain.user.UserRole;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    UserRole role
) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole()
        );
    }
}
