package com.deathstar.vader.board.service;

import com.deathstar.vader.board.domain.FieldConstants;
import com.deathstar.vader.loom.domain.Item;
import com.deathstar.vader.loom.engine.LexRank;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import com.deathstar.vader.loom.service.ItemQueryService;
import com.deathstar.vader.loom.service.LoomClient;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardTaskService {

    private final LoomClient loomClient;
    private final ItemQueryService itemQueryService;
    private final ScopedValueIdentityResolver identityResolver;

    @Transactional
    public com.deathstar.vader.dto.generated.Board createBoard(String title) {
        String tenantId = identityResolver.currentTenantId();
        UUID boardId = UUID.randomUUID();

        java.util.Map<UUID, Object> initialProperties = new java.util.HashMap<>();
        initialProperties.put(FieldConstants.TITLE_ID, title);
        initialProperties.put(FieldConstants.TYPE_ID, "Board");
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        initialProperties.put(FieldConstants.CREATED_AT_ID, now.toString());

        loomClient.createItemWithId(boardId, "Board", initialProperties);

        var board = new com.deathstar.vader.dto.generated.Board();
        board.setId(boardId);
        board.setTitle(title);
        board.setTenantId(tenantId);
        board.setCreatedAt(now);
        return board;
    }

    @Transactional
    public com.deathstar.vader.dto.generated.BoardColumn createColumn(
            UUID boardId, String title, Integer orderIndex) {
        UUID columnId = UUID.randomUUID();

        java.util.Map<UUID, Object> initialProperties = new java.util.HashMap<>();
        initialProperties.put(FieldConstants.TITLE_ID, title);
        initialProperties.put(FieldConstants.TYPE_ID, "BoardColumn");
        initialProperties.put(FieldConstants.BOARD_ID, boardId.toString());
        initialProperties.put(FieldConstants.ORDER_INDEX_ID, orderIndex.toString());
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        initialProperties.put(FieldConstants.CREATED_AT_ID, now.toString());

        loomClient.createItemWithId(columnId, "BoardColumn", initialProperties);

        var column = new com.deathstar.vader.dto.generated.BoardColumn();
        column.setId(columnId);
        column.setBoardId(boardId);
        column.setTitle(title);
        column.setOrderIndex(orderIndex);
        column.setCreatedAt(now);
        return column;
    }

    @Transactional
    public UUID createTask(UUID boardId, String title, UUID statusColumnId) {
        String tenantId = identityResolver.currentTenantId();
        UUID taskId = UUID.randomUUID();

        java.util.Map<UUID, Object> initialProperties = new java.util.HashMap<>();
        initialProperties.put(UUID.fromString("00000000-0000-0000-0000-000000000000"), title);
        initialProperties.put(FieldConstants.TYPE_ID, "BoardTask");
        initialProperties.put(FieldConstants.STATUS_ID, statusColumnId.toString());
        initialProperties.put(FieldConstants.LEXRANK_ID, LexRank.getMiddle(null, null));

        loomClient.createItemWithId(taskId, "BoardTask", initialProperties);

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

        java.util.Map<UUID, Object> updates = new java.util.HashMap<>();
        updates.put(FieldConstants.STATUS_ID, newStatusId.toString());
        updates.put(FieldConstants.LEXRANK_ID, newRank);

        loomClient.updateItem(taskId, updates, currentVersion);
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
        java.util.Map<UUID, Object> updates = new java.util.HashMap<>();

        if (title != null)
            updates.put(UUID.fromString("00000000-0000-0000-0000-000000000000"), title);
        if (description != null) updates.put(FieldConstants.DESCRIPTION_ID, description);
        if (priority != null) updates.put(FieldConstants.PRIORITY_ID, priority);
        if (dueDate != null) updates.put(FieldConstants.DUE_DATE_ID, dueDate);

        if (attributes != null) {
            for (java.util.Map.Entry<String, String> entry : attributes.entrySet()) {
                UUID dynamicFieldId =
                        UUID.nameUUIDFromBytes(
                                entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                updates.put(dynamicFieldId, entry.getValue());
            }
        }

        loomClient.updateItem(taskId, updates, currentVersion);
    }

    @Transactional(readOnly = true)
    public List<com.deathstar.vader.dto.generated.Board> getBoards() {
        List<Item> items =
                itemQueryService.getItemsByDynamicProperty(FieldConstants.TYPE_ID, "Board");
        return items.stream()
                .filter(item -> item.tenantId().equals(identityResolver.currentTenantId()))
                .map(
                        item -> {
                            var board = new com.deathstar.vader.dto.generated.Board();
                            board.setId(item.itemId());
                            board.setTenantId(item.tenantId());
                            var staticAttrs = item.staticAttributes();
                            if (staticAttrs.containsKey(FieldConstants.TITLE_ID)) {
                                board.setTitle(staticAttrs.get(FieldConstants.TITLE_ID).toString());
                            }
                            if (staticAttrs.containsKey(FieldConstants.CREATED_AT_ID)) {
                                board.setCreatedAt(
                                        java.time.OffsetDateTime.parse(
                                                staticAttrs
                                                        .get(FieldConstants.CREATED_AT_ID)
                                                        .toString()));
                            } else {
                                board.setCreatedAt(java.time.OffsetDateTime.now());
                            }
                            return board;
                        })
                .toList();
    }

    @Transactional(readOnly = true)
    public com.deathstar.vader.dto.generated.Board getBoardWithColumns(UUID boardId) {
        Item boardItem = itemQueryService.getItem(boardId);
        if (boardItem == null) throw new IllegalArgumentException("Board not found");

        var board = new com.deathstar.vader.dto.generated.Board();
        board.setId(boardItem.itemId());
        board.setTenantId(boardItem.tenantId());
        if (boardItem.staticAttributes().containsKey(FieldConstants.TITLE_ID)) {
            board.setTitle(boardItem.staticAttributes().get(FieldConstants.TITLE_ID).toString());
        }
        if (boardItem.staticAttributes().containsKey(FieldConstants.CREATED_AT_ID)) {
            board.setCreatedAt(
                    java.time.OffsetDateTime.parse(
                            boardItem
                                    .staticAttributes()
                                    .get(FieldConstants.CREATED_AT_ID)
                                    .toString()));
        } else {
            board.setCreatedAt(java.time.OffsetDateTime.now());
        }

        List<Item> colItems =
                itemQueryService.getItemsByDynamicProperty(
                        FieldConstants.BOARD_ID, boardId.toString());
        List<com.deathstar.vader.dto.generated.BoardColumn> columns =
                colItems.stream()
                        .filter(
                                item ->
                                        "BoardColumn"
                                                .equals(
                                                        item.dynamicAttributes()
                                                                .get(FieldConstants.TYPE_ID)))
                        .map(
                                item -> {
                                    var col = new com.deathstar.vader.dto.generated.BoardColumn();
                                    col.setId(item.itemId());
                                    col.setBoardId(boardId);
                                    if (item.staticAttributes()
                                            .containsKey(FieldConstants.TITLE_ID)) {
                                        col.setTitle(
                                                item.staticAttributes()
                                                        .get(FieldConstants.TITLE_ID)
                                                        .toString());
                                    }
                                    if (item.dynamicAttributes()
                                            .containsKey(FieldConstants.ORDER_INDEX_ID)) {
                                        col.setOrderIndex(
                                                Integer.parseInt(
                                                        item.dynamicAttributes()
                                                                .get(FieldConstants.ORDER_INDEX_ID)
                                                                .toString()));
                                    }
                                    if (item.staticAttributes()
                                            .containsKey(FieldConstants.CREATED_AT_ID)) {
                                        col.setCreatedAt(
                                                java.time.OffsetDateTime.parse(
                                                        item.staticAttributes()
                                                                .get(FieldConstants.CREATED_AT_ID)
                                                                .toString()));
                                    } else {
                                        col.setCreatedAt(java.time.OffsetDateTime.now());
                                    }
                                    return col;
                                })
                        .sorted(
                                java.util.Comparator.comparing(
                                        com.deathstar.vader.dto.generated.BoardColumn
                                                ::getOrderIndex,
                                        java.util.Comparator.nullsLast(
                                                java.util.Comparator.naturalOrder())))
                        .toList();

        board.setColumns(columns);
        return board;
    }

    @Transactional(readOnly = true)
    public List<com.deathstar.vader.dto.generated.BoardTask> getTasksForBoard(UUID boardId) {
        var board = getBoardWithColumns(boardId);
        List<String> columnIds =
                board.getColumns() != null
                        ? board.getColumns().stream().map(c -> c.getId().toString()).toList()
                        : List.of();

        if (columnIds.isEmpty()) return List.of();

        List<Item> items =
                itemQueryService.getItemsByDynamicPropertyIn(FieldConstants.STATUS_ID, columnIds);

        return items.stream()
                .map(
                        item -> {
                            com.deathstar.vader.dto.generated.BoardTask task =
                                    new com.deathstar.vader.dto.generated.BoardTask();
                            task.setId(item.itemId());
                            task.setVersion(item.version());
                            task.setType("BoardTask");

                            var staticAttrs = item.staticAttributes();
                            var dynamicAttrs = item.dynamicAttributes();

                            if (staticAttrs.containsKey(FieldConstants.TITLE_ID)) {
                                task.setTitle(staticAttrs.get(FieldConstants.TITLE_ID).toString());
                            }
                            if (staticAttrs.containsKey(FieldConstants.DESCRIPTION_ID)) {
                                task.setDescription(
                                        staticAttrs.get(FieldConstants.DESCRIPTION_ID).toString());
                            }
                            if (staticAttrs.containsKey(FieldConstants.PRIORITY_ID)) {
                                task.setPriority(
                                        staticAttrs.get(FieldConstants.PRIORITY_ID).toString());
                            }
                            if (staticAttrs.containsKey(FieldConstants.DUE_DATE_ID)) {
                                task.setDueDate(
                                        java.time.LocalDate.parse(
                                                staticAttrs
                                                        .get(FieldConstants.DUE_DATE_ID)
                                                        .toString()));
                            }

                            if (dynamicAttrs.containsKey(FieldConstants.STATUS_ID)) {
                                task.setStatus(
                                        UUID.fromString(
                                                dynamicAttrs
                                                        .get(FieldConstants.STATUS_ID)
                                                        .toString()));
                            }
                            if (dynamicAttrs.containsKey(FieldConstants.LEXRANK_ID)) {
                                task.setLexRank(
                                        dynamicAttrs.get(FieldConstants.LEXRANK_ID).toString());
                            }

                            java.util.Map<String, String> customAttributes =
                                    new java.util.HashMap<>();
                            for (java.util.Map.Entry<UUID, Object> entry :
                                    dynamicAttrs.entrySet()) {
                                if (!entry.getKey().equals(FieldConstants.STATUS_ID)
                                        && !entry.getKey().equals(FieldConstants.LEXRANK_ID)) {
                                    customAttributes.put(
                                            entry.getKey().toString(), entry.getValue().toString());
                                }
                            }
                            task.setAttributes(customAttributes);

                            return task;
                        })
                .toList();
    }
}
