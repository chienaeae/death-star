package com.deathstar.vader.workspace.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workspace_invitations")
@Getter
@Setter
public class WorkspaceInvitation {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    private String email;
    private String token;
    private String status;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
