package com.bcaas.core.infrastructure.persistence.actor;

import com.bcaas.core.actor.domain.model.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.AuditInfo;
import com.bcaas.core.shared.domain.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("bcaas_actors")
public class ActorEntity {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("email")
    private String email;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone_number")
    private String phoneNumber;

    @Column("locale")
    private String locale;

    @Column("role")
    private String role;

    @Column("status")
    private String status;

    @Column("bio")
    private String bio;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("location")
    private String location;

    @Column("created_at")
    private Instant createdAt;

    @Column("created_by")
    private UUID createdBy;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("updated_by")
    private UUID updatedBy;

    public static ActorEntity fromDomain(Actor actor) {
        ActorIdentity identity = actor.getIdentity();
        ActorProfile profile = actor.getProfile();
        AuditInfo audit = actor.getAuditInfo();

        return ActorEntity.builder()
                .id(actor.getId().value())
                .tenantId(actor.getTenantId().value())
                .email(identity.email())
                .firstName(identity.firstName())
                .lastName(identity.lastName())
                .phoneNumber(identity.phoneNumber())
                .locale(identity.locale())
                .role(actor.getRole().name())
                .status(actor.getStatus().name())
                .bio(profile.bio())
                .avatarUrl(profile.avatarUrl())
                .location(profile.location())
                .createdAt(audit.createdAt())
                .createdBy(audit.createdBy().value())
                .updatedAt(audit.updatedAt())
                .updatedBy(audit.updatedBy().value())
                .build();
    }

    public Actor toDomain() {
        ActorIdentity identity = new ActorIdentity(
                email, firstName, lastName, phoneNumber, locale
        );
        ActorProfile profile = new ActorProfile(bio, avatarUrl, location, java.util.Map.of());
        AuditInfo auditInfo = new AuditInfo(
                createdAt, ActorId.of(createdBy),
                updatedAt, ActorId.of(updatedBy)
        );
        return Actor.reconstitute(
                ActorId.of(id), TenantId.of(tenantId),
                identity, ActorRole.valueOf(role),
                ActorStatus.valueOf(status),
                profile, auditInfo
        );
    }
}
