package com.deathstar.vader.asset.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioStorageStrategyTest {

    @Mock
    private S3Presigner s3Presigner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGeneratePresignedUploadUrl_RewritesHostname() throws MalformedURLException {
        // Arrange
        URL internalDockerUrl = new URL("http://minio:9000/deathstar-assets/test-key.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=xyz");
        
        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(internalDockerUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        MinioStorageStrategy strategy = new MinioStorageStrategy(s3Presigner, "deathstar-assets", "http://localhost:9000");

        // Act
        URL resultUrl = strategy.generatePresignedUploadUrl("test-key.jpg", "image/jpeg", Duration.ofMinutes(15));

        // Assert
        assertThat(resultUrl.getHost()).isEqualTo("localhost");
        assertThat(resultUrl.getPort()).isEqualTo(9000);
        assertThat(resultUrl.getPath()).isEqualTo("/deathstar-assets/test-key.jpg");
        assertThat(resultUrl.getQuery()).contains("X-Amz-Signature=xyz");
        assertThat(resultUrl.getProtocol()).isEqualTo("http");
    }

    @Test
    void testGeneratePresignedUploadUrl_NoRewriteIfPublicUrlIsBlank() throws MalformedURLException {
        // Arrange
        URL internalDockerUrl = new URL("http://minio:9000/deathstar-assets/test-key.jpg?X-Amz-Signature=xyz");
        
        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(internalDockerUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        MinioStorageStrategy strategy = new MinioStorageStrategy(s3Presigner, "deathstar-assets", "");

        // Act
        URL resultUrl = strategy.generatePresignedUploadUrl("test-key.jpg", "image/jpeg", Duration.ofMinutes(15));

        // Assert
        assertThat(resultUrl.getHost()).isEqualTo("minio");
    }
}
