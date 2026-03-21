package com.deathstar.vader.workspace.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workspace_members")
@Getter
@Setter
public class WorkspaceMember {
    @EmbeddedId private WorkspaceMemberId id;

    private String role;

    @Column(name = "is_active")
    private Boolean isActive = false;
}
