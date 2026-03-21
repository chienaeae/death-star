package com.deathstar.vader.board.infrastructure;

import com.deathstar.vader.board.domain.FieldConstants;
import com.deathstar.vader.board.repository.TaskFieldDefinitionRepository;
import com.deathstar.vader.loom.domain.BucketType;
import com.deathstar.vader.loom.domain.FieldDefinition;
import com.deathstar.vader.loom.domain.FieldDefinition.FieldType;
import com.deathstar.vader.loom.spi.FieldRegistry;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardFieldRegistry implements FieldRegistry {

    private final TaskFieldDefinitionRepository repository;
    private final ConcurrentHashMap<UUID, FieldDefinition> localCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initStandardFields() {
        registerStandardField(
                FieldConstants.TITLE_ID, "Title", FieldType.STRING, BucketType.STATIC);
        registerStandardField(
                FieldConstants.DESCRIPTION_ID,
                "Description",
                FieldType.LONG_TEXT,
                BucketType.STATIC);
        registerStandardField(
                FieldConstants.STATUS_ID, "Status", FieldType.STATUS, BucketType.DYNAMIC);
        registerStandardField(
                FieldConstants.LEXRANK_ID, "LexRank", FieldType.LEXRANK, BucketType.DYNAMIC);
        registerStandardField(
                FieldConstants.PRIORITY_ID, "Priority", FieldType.STRING, BucketType.STATIC);
        registerStandardField(
                FieldConstants.DUE_DATE_ID, "DueDate", FieldType.STRING, BucketType.STATIC);
        registerStandardField(FieldConstants.TYPE_ID, "Type", FieldType.STRING, BucketType.DYNAMIC);
        registerStandardField(
                FieldConstants.BOARD_ID, "BoardId", FieldType.STRING, BucketType.DYNAMIC);
        registerStandardField(
                FieldConstants.ORDER_INDEX_ID, "OrderIndex", FieldType.INTEGER, BucketType.DYNAMIC);
        registerStandardField(
                FieldConstants.CREATED_AT_ID, "CreatedAt", FieldType.STRING, BucketType.STATIC);
        log.debug("Successfully initialized {} standard structural fields.", localCache.size());
    }

    private void registerStandardField(UUID id, String name, FieldType type, BucketType bucket) {
        localCache.put(id, new FieldDefinition(id, name, type, bucket));
    }

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
