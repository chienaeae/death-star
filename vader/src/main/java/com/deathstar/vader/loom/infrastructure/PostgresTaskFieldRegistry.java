package com.deathstar.vader.loom.infrastructure;

import com.deathstar.vader.loom.core.domain.FieldDefinition;
import com.deathstar.vader.loom.core.spi.FieldRegistry;
import com.deathstar.vader.loom.repository.TaskFieldDefinitionRepository;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresTaskFieldRegistry implements FieldRegistry {

    private final TaskFieldDefinitionRepository repository;
    private final ConcurrentHashMap<UUID, FieldDefinition> localCache = new ConcurrentHashMap<>();

    @Override
    public FieldDefinition getField(UUID fieldId) {
        return localCache.computeIfAbsent(
                fieldId,
                id -> {
                    log.debug("Cache miss for fieldId: {}. Fetching from PostgreSQL.", id);
                    return repository
                            .findById(id)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Unknown Field Definition: " + id));
                });
    }

    /**
     * Eagerly seeds the local cache. Called immediately after a field is created via the REST API.
     */
    public void primeCache(FieldDefinition definition) {
        localCache.put(definition.fieldId(), definition);
        log.debug("Successfully primed cache for new field: {}", definition.name());
    }
}
