package ru.mephi.identity.api.user;

import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.security.PrincipalIds;
import ru.mephi.identity.usecase.user.GetCurrentUserUseCase;
import ru.mephi.identity.usecase.user.GetUserUseCase;
import ru.mephi.identity.usecase.user.ListUsersUseCase;
import ru.mephi.identity.usecase.user.UpdateUserRoleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUser;
    private final ListUsersUseCase listUsers;
    private final GetUserUseCase getUser;
    private final UpdateUserRoleUseCase updateUserRole;

    public UserController(
        GetCurrentUserUseCase getCurrentUser,
        ListUsersUseCase listUsers,
        GetUserUseCase getUser,
        UpdateUserRoleUseCase updateUserRole
    ) {
        this.getCurrentUser = getCurrentUser;
        this.listUsers = listUsers;
        this.getUser = getUser;
        this.updateUserRole = updateUserRole;
    }

    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return getCurrentUser.execute(PrincipalIds.userId(jwt));
    }

    @Operation(summary = "List users")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> list(Pageable pageable) {
        return listUsers.execute(pageable);
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse get(@PathVariable UUID id) {
        return getUser.execute(id);
    }

    @Operation(summary = "Update user role")
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRoleRequest request,
        HttpServletRequest servletRequest
    ) {
        return updateUserRole.execute(id, request.role(), PrincipalIds.subject(jwt), WebRequestMetadata.from(servletRequest));
    }
}
