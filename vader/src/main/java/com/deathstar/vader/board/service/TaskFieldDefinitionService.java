package com.deathstar.vader.board.service;

import com.deathstar.vader.board.infrastructure.BoardFieldRegistry;
import com.deathstar.vader.board.repository.TaskFieldDefinitionRepository;
import com.deathstar.vader.loom.domain.BucketType;
import com.deathstar.vader.loom.domain.FieldDefinition;
import com.deathstar.vader.loom.domain.FieldDefinition.FieldType;
import com.deathstar.vader.loom.spi.IdentityResolver;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskFieldDefinitionService {

    private final TaskFieldDefinitionRepository repository;
    private final BoardFieldRegistry fieldRegistry;
    private final IdentityResolver identityResolver;

    @Transactional(readOnly = true)
    public List<FieldDefinition> getAllFields() {
        String tenantId = identityResolver.currentTenantId();
        return repository.findAllByTenantId(tenantId);
    }

    @Transactional
    public FieldDefinition createField(String name, FieldType type, BucketType overrideBucket) {
        String tenantId = identityResolver.currentTenantId();

        // 1. Generate UUID & Determine Bucket
        UUID fieldId = UUID.randomUUID();
        BucketType bucketType = overrideBucket;
        if (bucketType == null) {
            // Re-use core domain logic for inference
            bucketType = new FieldDefinition(fieldId, name, type, null).effectiveBucket();
        }

        FieldDefinition newField = new FieldDefinition(fieldId, name, type, bucketType);

        // 2. Save to Postgres
        repository.save(tenantId, newField);
        log.info("Created new custom field '{}' [{}] for tenant: {}", name, type, tenantId);

        // 3. Eagerly seed the local HashMap cache to avoid a future DB hit during stream
        // projection!
        fieldRegistry.primeCache(newField);

        return newField;
    }
}
