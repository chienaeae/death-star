package com.deathstar.vader.asset;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "asset_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AssetReference.AssetReferenceId.class)
public class AssetReference {

    @Id
    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Id
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Id
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetReferenceId implements Serializable {
        private UUID assetId;
        private String entityType;
        private String entityId;
    }
}
