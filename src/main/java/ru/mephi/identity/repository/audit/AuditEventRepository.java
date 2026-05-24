package ru.mephi.identity.repository.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.identity.domain.audit.AuditEventEntity;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
}
