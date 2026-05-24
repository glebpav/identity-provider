package ru.mephi.identity.service.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.mephi.identity.domain.audit.AuditEventEntity;
import ru.mephi.identity.repository.audit.AuditEventRepository;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(String actorId, String actorType, String eventType, String ipAddress, String metadata) {
        repository.save(new AuditEventEntity(UUID.randomUUID(), actorId, actorType, eventType, ipAddress, metadata));
    }
}
