package com.deathstar.vader.asset.storage;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
    public BlobStorage s3BlobStorage(StorageProperties properties) {
        log.info("Initializing S3StorageStrategy for bucket: {}", properties.getBucket());

        S3Presigner presigner =
                S3Presigner.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();

        return new S3StorageStrategy(presigner, properties.getBucket());
    }

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
    public BlobStorage minioBlobStorage(StorageProperties properties) {
        log.info(
                "Initializing MinioStorageStrategy for bucket: {} at endpoint: {}",
                properties.getBucket(),
                properties.getMinio().getEndpoint());

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey());

        // Use public URL for presigner endpoint if configured, because AWS SDK signs the Host
        // header based on it.
        // It's paramount that the signature uses the same host the frontend uses (localhost:9000).
        String presignerEndpoint =
                properties.getMinio().getPublicUrl() != null
                                && !properties.getMinio().getPublicUrl().isBlank()
                        ? properties.getMinio().getPublicUrl()
                        : properties.getMinio().getEndpoint();

        S3Presigner presigner =
                S3Presigner.builder()
                        .endpointOverride(URI.create(presignerEndpoint))
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .region(Region.US_EAST_1) // MinIO requires a generic region
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(true) // Crucial for MinIO!
                                        .build())
                        .build();

        return new MinioStorageStrategy(
                presigner, properties.getBucket(), properties.getMinio().getPublicUrl());
    }
}
