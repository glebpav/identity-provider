package ru.mephi.identity.api.user;

import jakarta.validation.constraints.NotNull;
import ru.mephi.identity.domain.user.UserRole;

public record UpdateUserRoleRequest(@NotNull UserRole role) {
}
