package com.deathstar.vader.board.service;

import com.deathstar.loom.core.domain.Event;
import com.deathstar.loom.core.engine.LexRank;
import com.deathstar.loom.core.engine.LoomEngine;
import com.deathstar.loom.core.spi.EventStore;
import com.deathstar.vader.board.entity.BoardColumn;
import com.deathstar.vader.board.repository.BoardColumnRepository;
import com.deathstar.vader.board.repository.BoardRepository;
import com.deathstar.vader.loom.domain.FieldConstants;
import com.deathstar.vader.loom.spi.ScopedValueIdentityResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardTaskService {

    private final LoomEngine loomEngine;
    private final EventStore eventStore;
    private final BoardColumnRepository columnRepository;
    private final BoardRepository boardRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ScopedValueIdentityResolver identityResolver;
    private final ObjectMapper objectMapper;

    @Transactional
    public com.deathstar.vader.board.entity.Board createBoard(String title) {
        String tenantId = identityResolver.currentTenantId();

        var board =
                com.deathstar.vader.board.entity.Board.builder()
                        .title(title)
                        .tenantId(tenantId)
                        .build();

        return boardRepository.save(board);
    }

    @Transactional
    public BoardColumn createColumn(UUID boardId, String title, Integer orderIndex) {
        var board =
                boardRepository
                        .findById(boardId)
                        .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        var column = BoardColumn.builder().board(board).title(title).orderIndex(orderIndex).build();

        return columnRepository.save(column);
    }

    @Transactional
    public UUID createTask(UUID boardId, String title, UUID statusColumnId) {
        String tenantId = identityResolver.currentTenantId();
        UUID taskId = UUID.randomUUID();

        // 1. Initial Creation translated to an Event Intent
        // The background worker will catch this and perform the INSERT on the next phase.
        eventStore.append(
                new Event(
                        UUID.randomUUID(), tenantId, taskId, null, "ITEM_CREATED", null, null, 0L));

        // 2. Set the static Title property via Loom
        // We use baseVersion 1 since the row will be created with version 1
        loomEngine.updateProperty(
                taskId, UUID.fromString("00000000-0000-0000-0000-000000000000"), title, 1L);

        // 3. Set the dynamic Status property via Loom
        // The baseVersion increments to 2 after the first update
        loomEngine.updateProperty(taskId, FieldConstants.STATUS_ID, statusColumnId.toString(), 2L);

        // 4. Set the initial LexRank via Loom
        loomEngine.updateProperty(
                taskId, FieldConstants.LEXRANK_ID, LexRank.getMiddle(null, null), 3L);

        return taskId;
    }

    @Transactional
    public void moveTask(
            UUID taskId,
            UUID newStatusId,
            String prevLexRank,
            String nextLexRank,
            long currentVersion) {
        // Find midpoint
        String newRank = LexRank.getMiddle(prevLexRank, nextLexRank);

        // Update status and lexRank via Loom properties (which enforces OCC internally)
        loomEngine.updateProperty(
                taskId, FieldConstants.STATUS_ID, newStatusId.toString(), currentVersion);
        loomEngine.updateProperty(taskId, FieldConstants.LEXRANK_ID, newRank, currentVersion + 1);
    }

    @Transactional
    public void updateTask(
            UUID taskId,
            String title,
            String description,
            String priority,
            String dueDate,
            java.util.Map<String, String> attributes,
            long currentVersion) {
        long version = currentVersion;

        if (title != null) {
            loomEngine.updateProperty(taskId, FieldConstants.TITLE_ID, title, version++);
        }
        if (description != null) {
            loomEngine.updateProperty(
                    taskId, FieldConstants.DESCRIPTION_ID, description, version++);
        }
        if (priority != null) {
            loomEngine.updateProperty(taskId, FieldConstants.PRIORITY_ID, priority, version++);
        }
        if (dueDate != null) {
            loomEngine.updateProperty(taskId, FieldConstants.DUE_DATE_ID, dueDate, version++);
        }

        if (attributes != null) {
            for (java.util.Map.Entry<String, String> entry : attributes.entrySet()) {
                // We generate a reproducible UUID from the string key for Loom Property Routing
                UUID dynamicFieldId =
                        UUID.nameUUIDFromBytes(
                                entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                loomEngine.updateProperty(taskId, dynamicFieldId, entry.getValue(), version++);
            }
        }
    }

    @Transactional(readOnly = true)
    public com.deathstar.vader.board.entity.Board getBoard(UUID boardId) {
        var board =
                boardRepository
                        .findById(boardId)
                        .orElseThrow(() -> new IllegalArgumentException("Board not found"));
        // Force initialize lazy collection
        board.getColumns().size();
        return board;
    }

    @Transactional(readOnly = true)
    public List<com.deathstar.vader.dto.generated.BoardTask> getTasksForBoard(UUID boardId) {
        com.deathstar.vader.board.entity.Board board = getBoard(boardId);
        List<String> columnIds =
                board.getColumns().stream().map(c -> c.getId().toString()).toList();

        if (columnIds.isEmpty()) return List.of();

        String tenantId = identityResolver.currentTenantId();

        String sql =
                "SELECT id, version, attr_static, attr_dynamic FROM items "
                        + "WHERE tenant_id = :tenant_id "
                        + "AND attr_dynamic->>:status_id IN (:column_ids)";

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("tenant_id", tenantId)
                        .addValue("status_id", FieldConstants.STATUS_ID.toString())
                        .addValue("column_ids", columnIds),
                (rs, rowNum) -> {
                    com.deathstar.vader.dto.generated.BoardTask task =
                            new com.deathstar.vader.dto.generated.BoardTask();
                    task.setId(UUID.fromString(rs.getString("id")));
                    task.setVersion(rs.getLong("version"));
                    task.setType("DefaultItem");

                    try {
                        JsonNode staticAttrs = objectMapper.readTree(rs.getString("attr_static"));
                        JsonNode dynamicAttrs = objectMapper.readTree(rs.getString("attr_dynamic"));

                        if (staticAttrs.has(FieldConstants.TITLE_ID.toString())) {
                            task.setTitle(
                                    staticAttrs.get(FieldConstants.TITLE_ID.toString()).asText());
                        }
                        if (staticAttrs.has(FieldConstants.DESCRIPTION_ID.toString())) {
                            task.setDescription(
                                    staticAttrs
                                            .get(FieldConstants.DESCRIPTION_ID.toString())
                                            .asText());
                        }
                        if (staticAttrs.has(FieldConstants.PRIORITY_ID.toString())) {
                            task.setPriority(
                                    staticAttrs
                                            .get(FieldConstants.PRIORITY_ID.toString())
                                            .asText());
                        }
                        if (staticAttrs.has(FieldConstants.DUE_DATE_ID.toString())) {
                            String dueDateStr =
                                    staticAttrs.get(FieldConstants.DUE_DATE_ID.toString()).asText();
                            // openapi-codegen maps "date" to java.time.LocalDate
                            task.setDueDate(java.time.LocalDate.parse(dueDateStr));
                        }

                        if (dynamicAttrs.has(FieldConstants.STATUS_ID.toString())) {
                            task.setStatus(
                                    UUID.fromString(
                                            dynamicAttrs
                                                    .get(FieldConstants.STATUS_ID.toString())
                                                    .asText()));
                        }
                        if (dynamicAttrs.has(FieldConstants.LEXRANK_ID.toString())) {
                            task.setLexRank(
                                    dynamicAttrs
                                            .get(FieldConstants.LEXRANK_ID.toString())
                                            .asText());
                        }

                        // Map all remaining unrecognized dynamic fields into the generic attributes
                        // map
                        java.util.Map<String, String> attributes = new java.util.HashMap<>();
                        java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields =
                                dynamicAttrs.fields();
                        while (fields.hasNext()) {
                            java.util.Map.Entry<String, JsonNode> field = fields.next();
                            String key = field.getKey();
                            if (!key.equals(FieldConstants.STATUS_ID.toString())
                                    && !key.equals(FieldConstants.LEXRANK_ID.toString())) {
                                attributes.put(key, field.getValue().asText());
                            }
                        }
                        task.setAttributes(attributes);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON", e);
                    }

                    return task;
                });
    }
}
