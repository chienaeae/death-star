package com.deathstar.vader.asset.repository;

import com.deathstar.vader.asset.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    
    Optional<Asset> findByS3Key(String s3Key);

    @Query("SELECT a FROM Asset a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Asset> findByIdAndNotDeleted(UUID id);
}
