package com.deathstar.vader.controller;

import com.deathstar.vader.api.BoardsApi;
import com.deathstar.vader.board.repository.BoardRepository;
import com.deathstar.vader.board.service.BoardTaskService;
import com.deathstar.vader.dto.generated.Board;
import com.deathstar.vader.dto.generated.BoardTaskCreateRequest;
import com.deathstar.vader.dto.generated.MoveTaskRequest;
import com.deathstar.vader.loom.spi.ScopedValueIdentityResolver;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardController implements BoardsApi {

    private final BoardRepository boardRepository;
    private final BoardTaskService boardTaskService;
    private final ScopedValueIdentityResolver identityResolver;

    @Override
    public ResponseEntity<List<Board>> boardsGet() {
        String tenantId = identityResolver.currentTenantId();
        List<com.deathstar.vader.board.entity.Board> dbBoards =
                boardRepository.findAllByTenantId(tenantId);

        List<Board> response =
                dbBoards.stream()
                        .map(
                                dbBoard -> {
                                    Board b = new Board();
                                    b.setId(dbBoard.getId());
                                    b.setTitle(dbBoard.getTitle());
                                    b.setTenantId(dbBoard.getTenantId());
                                    b.setCreatedAt(dbBoard.getCreatedAt());
                                    return b;
                                })
                        .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Board> boardsPost(
            com.deathstar.vader.dto.generated.BoardCreateRequest request) {
        var dbBoard = boardTaskService.createBoard(request.getTitle());
        Board response = new Board();
        response.setId(dbBoard.getId());
        response.setTitle(dbBoard.getTitle());
        response.setTenantId(dbBoard.getTenantId());
        response.setCreatedAt(dbBoard.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.BoardColumn> boardsBoardIdColumnsPost(
            UUID boardId, com.deathstar.vader.dto.generated.BoardColumnCreateRequest request) {
        var dbColumn =
                boardTaskService.createColumn(boardId, request.getTitle(), request.getOrderIndex());

        com.deathstar.vader.dto.generated.BoardColumn response =
                new com.deathstar.vader.dto.generated.BoardColumn();
        response.setId(dbColumn.getId());
        response.setBoardId(dbColumn.getBoard().getId());
        response.setTitle(dbColumn.getTitle());
        response.setOrderIndex(dbColumn.getOrderIndex());
        response.setCreatedAt(dbColumn.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Board> boardsBoardIdGet(UUID boardId) {
        var dbBoard = boardTaskService.getBoard(boardId);
        var tasks = boardTaskService.getTasksForBoard(boardId);

        Board response = new Board();
        response.setId(dbBoard.getId());
        response.setTitle(dbBoard.getTitle());
        response.setTenantId(dbBoard.getTenantId());
        response.setCreatedAt(dbBoard.getCreatedAt());

        if (dbBoard.getColumns() != null) {
            response.setColumns(
                    dbBoard.getColumns().stream()
                            .map(
                                    col -> {
                                        var dtoCol =
                                                new com.deathstar.vader.dto.generated.BoardColumn();
                                        dtoCol.setId(col.getId());
                                        dtoCol.setBoardId(col.getBoard().getId());
                                        dtoCol.setTitle(col.getTitle());
                                        dtoCol.setOrderIndex(col.getOrderIndex());
                                        dtoCol.setCreatedAt(col.getCreatedAt());

                                        var colTasks =
                                                tasks.stream()
                                                        .filter(
                                                                t ->
                                                                        t.getStatus()
                                                                                .equals(
                                                                                        col
                                                                                                .getId()))
                                                        .sorted(
                                                                java.util.Comparator.comparing(
                                                                        com.deathstar.vader.dto
                                                                                        .generated
                                                                                        .BoardTask
                                                                                ::getLexRank))
                                                        .toList();
                                        dtoCol.setTasks(colTasks);
                                        return dtoCol;
                                    })
                            .toList());
        }

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<String> boardsBoardIdTasksPost(
            UUID boardId, BoardTaskCreateRequest request) {
        UUID taskId = boardTaskService.createTask(boardId, request.getTitle(), request.getStatus());
        return ResponseEntity.ok(taskId.toString());
    }

    @Override
    public ResponseEntity<Void> boardsBoardIdTasksTaskIdMovePost(
            UUID boardId, UUID taskId, MoveTaskRequest request) {
        // We assume currentVersion is derived from the UI's last known state, hardcoded to 0 for
        // demo if missing
        boardTaskService.moveTask(
                taskId,
                request.getNewStatusId(),
                request.getPrevLexRank(),
                request.getNextLexRank(),
                request.getCurrentVersion());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.BoardTask> boardsBoardIdTasksTaskIdPut(
            UUID boardId,
            UUID taskId,
            com.deathstar.vader.dto.generated.BoardTaskUpdateRequest request) {
        boardTaskService.updateTask(
                taskId,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate() != null ? request.getDueDate().toString() : null,
                request.getAttributes(),
                request.getCurrentVersion());

        // After the update is pipelined to the engine, fetch the updated list and return the task
        // Note: For a strictly CQRS system this might be eventually consistent, but we are
        // simulating a strong read here
        List<com.deathstar.vader.dto.generated.BoardTask> tasks =
                boardTaskService.getTasksForBoard(boardId);
        com.deathstar.vader.dto.generated.BoardTask updatedTask =
                tasks.stream()
                        .filter(t -> t.getId().equals(taskId))
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalArgumentException("Task not found after update"));

        return ResponseEntity.ok(updatedTask);
    }
}
