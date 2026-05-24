package ru.mephi.identity.repository.auth;

import ru.mephi.identity.domain.auth.RefreshTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}
