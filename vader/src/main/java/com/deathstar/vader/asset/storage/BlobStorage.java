package com.deathstar.vader.asset.storage;

import java.net.URL;
import java.time.Duration;

/**
 * Abstraction layer for blob storage operations.
 * Implemented by S3 or MinIO strategies based on the environment.
 */
public interface BlobStorage {

    /**
     * Generates a pre-signed URL for client-side direct uploads to the storage provider.
     *
     * @param objectKey   The unique identifier for the object within the bucket.
     * @param contentType The MIME type of the file.
     * @param ttl         The time-to-live duration for the presigned URL.
     * @return The pre-signed URL that can be used for an HTTP PUT request.
     */
    URL generatePresignedUploadUrl(String objectKey, String contentType, Duration ttl);
}
