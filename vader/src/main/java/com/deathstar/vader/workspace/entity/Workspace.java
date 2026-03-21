package com.deathstar.vader.workspace.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
public class Workspace {
    @Id private UUID id;
    private String name;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}
