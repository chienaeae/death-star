package com.deathstar.vader.asset.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.deathstar.vader.VaderApplication;
import com.deathstar.vader.asset.repository.AssetRepository;
import com.deathstar.vader.dto.generated.PresignedUrlRequest;
import com.deathstar.vader.dto.generated.PresignedUrlResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = VaderApplication.class)
@ActiveProfiles("e2e")
class AssetControllerE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("deathstar_e2e")
                    .withUsername("postgres")
                    .withPassword("password");

    @Container
    static GenericContainer<?> minio =
            new GenericContainer<>(
                            DockerImageName.parse("minio/minio:RELEASE.2024-03-30T09-41-56Z"))
                    .withExposedPorts(9000)
                    .withEnv("MINIO_ROOT_USER", "admin")
                    .withEnv("MINIO_ROOT_PASSWORD", "password")
                    .withCommand("server /data")
                    .waitingFor(Wait.forListeningPort());

    @Container
    static GenericContainer<?> nats =
            new GenericContainer<>(DockerImageName.parse("nats:2.10-alpine"))
                    .withExposedPorts(4222)
                    .waitingFor(Wait.forLogMessage(".*Listening for client connections.*\\n", 1));

    @Container
    static GenericContainer<?> clickhouse =
            new GenericContainer<>(
                            DockerImageName.parse("clickhouse/clickhouse-server:24.3-alpine"))
                    .withExposedPorts(8123, 9000)
                    .withEnv("CLICKHOUSE_USER", "default")
                    .withEnv("CLICKHOUSE_PASSWORD", "password")
                    .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
                    .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add(
                "storage.minio.endpoint",
                () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add(
                "storage.minio.publicUrl",
                () ->
                        "http://"
                                + minio.getHost()
                                + ":"
                                + minio.getMappedPort(
                                        9000)); // We use dynamic port for client test too

        registry.add(
                "audit.clickhouse.jdbc-url",
                () ->
                        "jdbc:ch:http://"
                                + clickhouse.getHost()
                                + ":"
                                + clickhouse.getMappedPort(8123)
                                + "/default");
        registry.add("nats.url", () -> "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222));
    }

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private AssetRepository assetRepository;

    private String getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body =
                "{ \"email\": \"jedi-"
                        + UUID.randomUUID()
                        + "@order.com\", \"password\": \"force\" }";
        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/auth/register", new HttpEntity<>(body, headers), String.class);

        String resBody = response.getBody();
        if (resBody != null && resBody.contains("accessToken\":\"")) {
            int start = resBody.indexOf("accessToken\":\"") + 14;
            int end = resBody.indexOf("\"", start);
            return resBody.substring(start, end);
        }
        return "UNKNOWN_TOKEN";
    }

    @BeforeAll
    static void setupMinioBucket() throws Exception {
        // We must create the bucket manually since minio-init script isn't in Testcontainers setup
        org.testcontainers.containers.Container.ExecResult result =
                minio.execInContainer(
                        "sh",
                        "-c",
                        "mkdir -p /data/deathstar-assets && curl -X PUT http://127.0.0.1:9000/deathstar-assets/");
        // Note: minio client native way might be needed if curl fails but usually minio creates
        // buckets if folder exists inside /data
    }

    @Test
    void e2eUploadFlow_TokenTamperingAndHappyPath() throws Exception {
        String token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // 1. Request Presigned URL
        PresignedUrlRequest req = new PresignedUrlRequest();
        req.setFilename("sabermetrics.csv");
        req.setContentType("text/csv");

        ResponseEntity<PresignedUrlResponse> urlResponse =
                restTemplate.postForEntity(
                        "/assets/upload-url",
                        new HttpEntity<>(req, headers),
                        PresignedUrlResponse.class);

        assertThat(urlResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PresignedUrlResponse res = urlResponse.getBody();
        assertThat(res).isNotNull();

        String uploadUrlStr = res.getUploadUrl();
        // The URL is double-encoded due to JSON serialization or Spring Test RestTemplate internals
        // (%253B instead of %3B).
        // Let's strip the double encoding to pass the raw URL correctly to HttpURLConnection.
        uploadUrlStr = uploadUrlStr.replace("%25", "%");

        // 2. Validate DB was written to (Asset State Management)
        assertThat(assetRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);

        // 3. TAMPERING TEST (Negative)
        String tamperedUrl =
                uploadUrlStr.replaceFirst("X-Amz-Signature=[0-9a-f]+", "X-Amz-Signature=1");
        if (tamperedUrl.equals(uploadUrlStr)) {
            tamperedUrl =
                    uploadUrlStr.replaceFirst("x-amz-signature=[0-9a-f]+", "x-amz-signature=1");
        }

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.valueOf("text/csv"));
        HttpEntity<byte[]> uploadEntity =
                new HttpEntity<>(
                        "A,B,C\n1,2,3".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        uploadHeaders);

        ResponseEntity<String> tamperResponse =
                restTemplate.exchange(
                        new java.net.URI(tamperedUrl), HttpMethod.PUT, uploadEntity, String.class);
        assertThat(tamperResponse.getStatusCode().is4xxClientError()).isTrue();
        assertThat(tamperResponse.getBody()).contains("SignatureDoesNotMatch");

        // 4. HAPPY PATH (Successful direct upload)
        ResponseEntity<String> successResponse =
                restTemplate.exchange(
                        new java.net.URI(uploadUrlStr), HttpMethod.PUT, uploadEntity, String.class);
        assertThat(successResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
