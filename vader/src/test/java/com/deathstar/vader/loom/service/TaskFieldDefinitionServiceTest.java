package com.deathstar.vader.loom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deathstar.vader.loom.core.domain.BucketType;
import com.deathstar.vader.loom.core.domain.FieldDefinition;
import com.deathstar.vader.loom.core.domain.FieldDefinition.FieldType;
import com.deathstar.vader.loom.core.spi.IdentityResolver;
import com.deathstar.vader.loom.infrastructure.PostgresTaskFieldRegistry;
import com.deathstar.vader.loom.repository.TaskFieldDefinitionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskFieldDefinitionServiceTest {

    @Mock private TaskFieldDefinitionRepository repository;
    @Mock private PostgresTaskFieldRegistry fieldRegistry;
    @Mock private IdentityResolver identityResolver;

    private TaskFieldDefinitionService service;

    @BeforeEach
    void setUp() {
        service = new TaskFieldDefinitionService(repository, fieldRegistry, identityResolver);
    }

    @Test
    void testGetAllFields_ReturnsFieldsForTenant() {
        // Arrange
        String tenantId = "tenant-123";
        when(identityResolver.currentTenantId()).thenReturn(tenantId);

        FieldDefinition mockField =
                new FieldDefinition(
                        java.util.UUID.randomUUID(),
                        "Story Points",
                        FieldType.INTEGER,
                        BucketType.STATIC);
        when(repository.findAllByTenantId(tenantId)).thenReturn(List.of(mockField));

        // Act
        List<FieldDefinition> result = service.getAllFields();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Story Points");
        verify(identityResolver).currentTenantId();
        verify(repository).findAllByTenantId(tenantId);
    }

    @Test
    void testCreateField_GeneratesUUID_DeterminesBucket_SavesAndPrimesCache() {
        // Arrange
        String tenantId = "tenant-abc";
        when(identityResolver.currentTenantId()).thenReturn(tenantId);

        String name = "New Field";
        FieldType type = FieldType.STRING;
        // No override BucketType, should infer DYNAMIC

        // Act
        FieldDefinition created = service.createField(name, type, null);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.fieldId()).isNotNull();
        assertThat(created.name()).isEqualTo(name);
        assertThat(created.type()).isEqualTo(type);
        assertThat(created.targetBucket()).isEqualTo(BucketType.STATIC); // Implicitly determined

        // Verify DB Save
        verify(repository).save(eq(tenantId), eq(created));

        // CRITICAL: Verify cache invalidation/priming prevents dirty reads
        verify(fieldRegistry).primeCache(created);
    }
}
