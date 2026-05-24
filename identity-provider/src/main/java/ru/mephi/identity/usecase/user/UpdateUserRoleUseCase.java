package ru.mephi.identity.usecase.user;

import ru.mephi.identity.api.user.UserResponse;
import ru.mephi.identity.common.error.ApiException;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.domain.user.UserRole;
import ru.mephi.identity.repository.user.UserRepository;
import ru.mephi.identity.service.audit.AuditService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserRoleUseCase {

    private final UserRepository users;
    private final AuditService auditService;

    public UpdateUserRoleUseCase(UserRepository users, AuditService auditService) {
        this.users = users;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse execute(UUID userId, UserRole role, String actorId, WebRequestMetadata metadata) {
        var user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        user.setRole(role);
        auditService.record(actorId, "USER", "USER_ROLE_UPDATED", metadata.ipAddress(), "targetUserId=" + userId + ",role=" + role);
        return UserResponse.from(user);
    }
}
