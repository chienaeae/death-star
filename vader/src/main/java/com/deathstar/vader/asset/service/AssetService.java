package com.deathstar.vader.asset.service;

import com.deathstar.vader.asset.Asset;
import com.deathstar.vader.asset.AssetReference;
import com.deathstar.vader.asset.AssetStatus;
import com.deathstar.vader.asset.repository.AssetReferenceRepository;
import com.deathstar.vader.asset.repository.AssetRepository;
import com.deathstar.vader.asset.storage.BlobStorage;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final BlobStorage blobStorage;
    private final AssetRepository assetRepository;
    private final AssetReferenceRepository assetReferenceRepository;

    /**
     * Pre-registers an asset and returns the Presigned URL. Generates a unique S3 key using the
     * active user's ID and original filename.
     */
    @Transactional
    public UploadInfo generateUploadUrl(UUID userId, String filename, String contentType) {
        log.info(
                "[ASSET_INIT] Pre-registering new asset upload for user {} with file {}",
                userId,
                filename);

        // Simple unique key generation: userId/uuid-filename
        // In reality, you'd sanitize the filename to prevent path traversal
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String s3Key = userId.toString() + "/" + UUID.randomUUID() + "-" + sanitizedFilename;

        Asset asset =
                Asset.builder()
                        .ownerId(userId)
                        .status(AssetStatus.INIT)
                        .s3Key(s3Key)
                        .mimeType(contentType)
                        .refCount(0)
                        .build();

        assetRepository.save(asset);

        // Generate 15-minute presigned URL
        URL presignedUrl =
                blobStorage.generatePresignedUploadUrl(s3Key, contentType, Duration.ofMinutes(15));

        log.info(
                "[UPLOAD_URL_GENERATED] URL generated for Asset ID: {} | Key: {}",
                asset.getId(),
                s3Key);

        return new UploadInfo(presignedUrl, asset.getId());
    }

    /** Links an asset to a specific entity, incrementing its reference count. */
    @Transactional
    public void linkAsset(UUID assetId, String entityType, String entityId) {
        log.info("[ASSET_LINK] Linking asset {} to {}/{}", assetId, entityType, entityId);

        Asset asset =
                assetRepository
                        .findByIdAndNotDeleted(assetId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Asset not found or deleted"));

        AssetReference ref =
                AssetReference.builder()
                        .assetId(assetId)
                        .entityType(entityType)
                        .entityId(entityId)
                        .build();

        assetReferenceRepository.save(ref);

        asset.setRefCount(asset.getRefCount() + 1);

        if (asset.getStatus() != AssetStatus.LINKED) {
            asset.setStatus(AssetStatus.LINKED);
        }
    }

    /** Unlinks an asset from a specific entity, decrementing its reference count. */
    @Transactional
    public void unlinkAsset(UUID assetId, String entityType, String entityId) {
        log.info("[ASSET_UNLINK] Unlinking asset {} from {}/{}", assetId, entityType, entityId);

        AssetReference.AssetReferenceId refId =
                new AssetReference.AssetReferenceId(assetId, entityType, entityId);
        Optional<AssetReference> optRef = assetReferenceRepository.findById(refId);

        if (optRef.isPresent()) {
            assetReferenceRepository.delete(optRef.get());

            Asset asset = assetRepository.findById(assetId).orElseThrow();
            asset.setRefCount(Math.max(0, asset.getRefCount() - 1));

            if (asset.getRefCount() == 0) {
                // If we want bucket notifications to mark UPLOADED, we only revert mapping status
                log.info("[ASSET_ORPHANED] Asset {} reference count reached 0", assetId);
            }
        }
    }
}
