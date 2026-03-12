package com.deathstar.vader.asset.repository;

import com.deathstar.vader.asset.AssetReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetReferenceRepository extends JpaRepository<AssetReference, AssetReference.AssetReferenceId> {
    
    List<AssetReference> findByAssetId(UUID assetId);
    
    List<AssetReference> findByEntityTypeAndEntityId(String entityType, String entityId);
}
