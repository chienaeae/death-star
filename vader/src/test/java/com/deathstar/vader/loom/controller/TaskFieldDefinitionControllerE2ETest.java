package com.deathstar.vader.loom.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.deathstar.vader.VaderApplication;
import com.deathstar.vader.dto.generated.BucketType;
import com.deathstar.vader.dto.generated.FieldType;
import com.deathstar.vader.dto.generated.TaskFieldDefinition;
import com.deathstar.vader.dto.generated.TaskFieldDefinitionRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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
class TaskFieldDefinitionControllerE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("deathstar_e2e")
                    .withUsername("postgres")
                    .withPassword("password");

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
                "audit.clickhouse.jdbc-url",
                () ->
                        "jdbc:ch:http://"
                                + clickhouse.getHost()
                                + ":"
                                + clickhouse.getMappedPort(8123)
                                + "/default");
        registry.add("nats.url", () -> "nats://" + nats.getHost() + ":" + nats.getMappedPort(4222));

        // Mock minio to prevent startup failures in e2e profile
        registry.add("storage.minio.endpoint", () -> "http://localhost:9000");
        registry.add("storage.minio.publicUrl", () -> "http://localhost:9000");
    }

    @Autowired private TestRestTemplate restTemplate;

    private String registerAndGetToken(String emailPrefix) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body =
                "{ \"email\": \""
                        + emailPrefix
                        + "-"
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

    @Test
    void testTenantIsolationForCustomFields() {
        // 1. Setup two different tenant users
        String tokenA = registerAndGetToken("tenantA");
        String tokenB = registerAndGetToken("tenantB");

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        headersA.setContentType(MediaType.APPLICATION_JSON);

        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        headersB.setContentType(MediaType.APPLICATION_JSON);

        // 2. Tenant A creates "Story Points"
        TaskFieldDefinitionRequest reqA =
                new TaskFieldDefinitionRequest()
                        .name("Story Points")
                        .fieldType(FieldType.INTEGER)
                        .bucketType(BucketType.STATIC);

        ResponseEntity<TaskFieldDefinition> createResA =
                restTemplate.postForEntity(
                        "/tasks/fields",
                        new HttpEntity<>(reqA, headersA),
                        TaskFieldDefinition.class);

        assertThat(createResA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TaskFieldDefinition fieldA = createResA.getBody();
        assertThat(fieldA).isNotNull();
        assertThat(fieldA.getName()).isEqualTo("Story Points");

        // 3. Tenant B creates "Client Name"
        TaskFieldDefinitionRequest reqB =
                new TaskFieldDefinitionRequest()
                        .name("Client Name")
                        .fieldType(FieldType.STRING)
                        .bucketType(BucketType.DYNAMIC);

        ResponseEntity<TaskFieldDefinition> createResB =
                restTemplate.postForEntity(
                        "/tasks/fields",
                        new HttpEntity<>(reqB, headersB),
                        TaskFieldDefinition.class);

        assertThat(createResB.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. Verify Isolation
        ResponseEntity<List<TaskFieldDefinition>> getResA =
                restTemplate.exchange(
                        "/tasks/fields",
                        HttpMethod.GET,
                        new HttpEntity<>(headersA),
                        new ParameterizedTypeReference<>() {});

        ResponseEntity<List<TaskFieldDefinition>> getResB =
                restTemplate.exchange(
                        "/tasks/fields",
                        HttpMethod.GET,
                        new HttpEntity<>(headersB),
                        new ParameterizedTypeReference<>() {});

        List<TaskFieldDefinition> fieldsA = getResA.getBody();
        List<TaskFieldDefinition> fieldsB = getResB.getBody();

        assertThat(fieldsA).isNotNull();
        assertThat(fieldsB).isNotNull();

        // Both should have system defaults (e.g. Title, Status) + their 1 custom field
        boolean hasStoryPointsInA =
                fieldsA.stream().anyMatch(f -> "Story Points".equals(f.getName()));
        boolean hasClientNameInA =
                fieldsA.stream().anyMatch(f -> "Client Name".equals(f.getName()));

        boolean hasClientNameInB =
                fieldsB.stream().anyMatch(f -> "Client Name".equals(f.getName()));
        boolean hasStoryPointsInB =
                fieldsB.stream().anyMatch(f -> "Story Points".equals(f.getName()));

        assertThat(hasStoryPointsInA).isTrue();
        assertThat(hasClientNameInA).isFalse(); // Isolated

        assertThat(hasClientNameInB).isTrue();
        assertThat(hasStoryPointsInB).isFalse(); // Isolated
    }
}
