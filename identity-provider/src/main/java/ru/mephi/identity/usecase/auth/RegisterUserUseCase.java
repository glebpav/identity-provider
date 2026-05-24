package ru.mephi.identity.usecase.auth;

import ru.mephi.identity.service.audit.AuditService;
import ru.mephi.identity.api.auth.RegisterRequest;
import ru.mephi.identity.common.error.ApiException;
import ru.mephi.identity.common.web.WebRequestMetadata;
import ru.mephi.identity.api.user.UserResponse;
import ru.mephi.identity.domain.user.UserEntity;
import ru.mephi.identity.domain.user.UserRole;
import ru.mephi.identity.domain.user.UserStatus;
import ru.mephi.identity.repository.user.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public RegisterUserUseCase(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        AuditService auditService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse execute(RegisterRequest request, WebRequestMetadata metadata) {
        var email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
        }
        var user = new UserEntity(
            UUID.randomUUID(),
            email,
            request.firstName().trim(),
            request.lastName().trim(),
            passwordEncoder.encode(request.password()),
            UserRole.DEFAULT,
            UserStatus.ACTIVE
        );
        var saved = users.save(user);
        auditService.record(saved.getId().toString(), "USER", "USER_REGISTERED", metadata.ipAddress(), "email=" + email);
        return UserResponse.from(saved);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
