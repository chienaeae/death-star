package com.deathstar.vader.board.controller;

import com.deathstar.vader.api.BoardsApi;
// board details
import com.deathstar.vader.board.service.BoardTaskService;
import com.deathstar.vader.dto.generated.Board;
import com.deathstar.vader.dto.generated.BoardTaskCreateRequest;
import com.deathstar.vader.dto.generated.MoveTaskRequest;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardController implements BoardsApi {

    // Repos removed around here
    private final BoardTaskService boardTaskService;
    private final ScopedValueIdentityResolver identityResolver;

    @Override
    public ResponseEntity<List<Board>> boardsGet() {
        List<Board> response = boardTaskService.getBoards();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Board> boardsPost(
            com.deathstar.vader.dto.generated.BoardCreateRequest request) {
        var dtoBoard = boardTaskService.createBoard(request.getTitle());
        return ResponseEntity.ok(dtoBoard);
    }

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.BoardColumn> boardsBoardIdColumnsPost(
            UUID boardId, com.deathstar.vader.dto.generated.BoardColumnCreateRequest request) {
        var dtoColumn =
                boardTaskService.createColumn(boardId, request.getTitle(), request.getOrderIndex());

        return ResponseEntity.ok(dtoColumn);
    }

    @Override
    public ResponseEntity<Board> boardsBoardIdGet(UUID boardId) {
        var response = boardTaskService.getBoardWithColumns(boardId);
        var tasks = boardTaskService.getTasksForBoard(boardId);

        if (response.getColumns() != null) {
            for (var col : response.getColumns()) {
                var colTasks =
                        tasks.stream()
                                .filter(t -> t.getStatus().equals(col.getId()))
                                .sorted(
                                        java.util.Comparator.comparing(
                                                com.deathstar.vader.dto.generated.BoardTask::getLexRank,
                                                java.util.Comparator.nullsLast(
                                                        java.util.Comparator.naturalOrder())))
                                .toList();
                col.setTasks(colTasks);
            }
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
