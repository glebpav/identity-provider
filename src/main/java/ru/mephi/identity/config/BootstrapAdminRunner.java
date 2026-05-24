package ru.mephi.identity.config;

import ru.mephi.identity.domain.user.UserEntity;
import ru.mephi.identity.domain.user.UserRole;
import ru.mephi.identity.domain.user.UserStatus;
import ru.mephi.identity.repository.user.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private final String adminEmail;
    private final String adminPassword;
    private final String adminFirstName;
    private final String adminLastName;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(
        @Value("${identity.bootstrap.admin-email:}") String adminEmail,
        @Value("${identity.bootstrap.admin-password:}") String adminPassword,
        @Value("${identity.bootstrap.admin-first-name:Admin}") String adminFirstName,
        @Value("${identity.bootstrap.admin-last-name:User}") String adminLastName,
        UserRepository users,
        PasswordEncoder passwordEncoder
    ) {
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        var email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        var admin = new UserEntity(
            UUID.randomUUID(),
            email,
            adminFirstName,
            adminLastName,
            passwordEncoder.encode(adminPassword),
            UserRole.ADMIN,
            UserStatus.ACTIVE
        );
        users.save(admin);
    }
}
