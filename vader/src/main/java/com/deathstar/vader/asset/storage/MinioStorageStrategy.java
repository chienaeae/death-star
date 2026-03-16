package com.deathstar.vader.asset.storage;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
public class MinioStorageStrategy implements BlobStorage {

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String publicUrl;

    public MinioStorageStrategy(S3Presigner s3Presigner, String bucketName, String publicUrl) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
    }

    @Override
    public URL generatePresignedUploadUrl(String objectKey, String contentType, Duration ttl) {
        log.debug("Generating MinIO presigned URL for key: {}", objectKey);

        PutObjectRequest objectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .putObjectRequest(objectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        URL internalUrl = presignedRequest.url();

        // Hostname mapping for Docker/Local environments
        // Internal URL points to http://minio:9000/bucket/key
        // We need to rewrite the authority to localhost:9000 for client direct upload
        if (publicUrl != null && !publicUrl.isBlank()) {
            return rewriteUrlForClient(internalUrl, publicUrl);
        }

        return internalUrl;
    }

    private URL rewriteUrlForClient(URL originalUrl, String overrideAuthority) {
        try {
            // E.g. target format: http://localhost:9000 or https://minio.local
            URI overrideUri = URI.create(overrideAuthority);

            String originalStr = originalUrl.toString();
            String protocol = originalUrl.getProtocol();
            String authority = originalUrl.getAuthority();

            String newProtocol =
                    overrideUri.getScheme() != null ? overrideUri.getScheme() : protocol;
            String newAuthority =
                    overrideUri.getAuthority() != null ? overrideUri.getAuthority() : authority;

            // Replace the start of the URL: protocol://authority (e.g., http://minio:9000)
            String targetPrefix = protocol + "://" + authority;
            String newPrefix = newProtocol + "://" + newAuthority;

            String newUrlStr =
                    originalStr.replaceFirst(
                            java.util.regex.Pattern.quote(targetPrefix), newPrefix);
            return new URL(newUrlStr);
        } catch (Exception e) {
            log.warn(
                    "Failed to rewrite MinIO URL. Falling back to internal URL {}: {}",
                    originalUrl,
                    e.getMessage());
            return originalUrl;
        }
    }

    @Override
    public String getPublicUrl(String objectKey) {
        String baseUri =
                this.publicUrl != null && !this.publicUrl.isBlank()
                        ? this.publicUrl
                        : "http://localhost:9000";
        // Ensure not duplicating trailing or preceding slashes.
        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }
        return String.format("%s/%s/%s", baseUri, bucketName, objectKey);
    }
}
