package ru.mephi.identity.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id", length = 128)
    private String actorId;

    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(UUID id, String actorId, String actorType, String eventType, String ipAddress, String metadata) {
        this.id = id;
        this.actorId = actorId;
        this.actorType = actorType;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
