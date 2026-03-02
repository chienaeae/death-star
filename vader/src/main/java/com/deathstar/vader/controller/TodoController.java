package com.deathstar.vader.controller;

import com.deathstar.vader.api.TodosApi;
import com.deathstar.vader.domain.Todo;
import com.deathstar.vader.dto.EventMessage;
import com.deathstar.vader.dto.generated.TodoRequest;
import com.deathstar.vader.repository.TodoRepository;
import com.deathstar.vader.service.SseBroadcasterService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class TodoController implements TodosApi {

    private final TodoRepository todoRepository;
    private final SseBroadcasterService sseService;

    @Override
    public ResponseEntity<List<com.deathstar.vader.dto.generated.Todo>> todosGet() {
        List<com.deathstar.vader.dto.generated.Todo> response =
                todoRepository.findAllByOrderByCreatedAtDesc().stream()
                        .map(this::mapDomainToDto)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<com.deathstar.vader.dto.generated.Todo> todosPost(
            TodoRequest todoRequest) {
        // 1. Mutate state
        Todo newTodo = new Todo(todoRequest.getTitle());
        Todo savedTodo = todoRepository.save(newTodo);

        // 2. Fire and forget event
        sseService.publishEvent(EventMessage.created(savedTodo));

        return ResponseEntity.status(HttpStatus.CREATED).body(mapDomainToDto(savedTodo));
    }

    @Override
    public ResponseEntity<SseEmitter> eventsGet() {
        return ResponseEntity.ok(sseService.registerClient());
    }

    // --- Mapper (in large projects, usually abstracted to MapStruct or a dedicated Mapper layer)
    // ---
    private com.deathstar.vader.dto.generated.Todo mapDomainToDto(Todo entity) {
        return new com.deathstar.vader.dto.generated.Todo()
                .id(entity.getId())
                .title(entity.getTitle())
                .completed(entity.isCompleted())
                // Assuming entity.getCreatedAt() is LocalDateTime or Instant, convert to
                // OffsetDateTime
                .createdAt(
                        entity.getCreatedAt() != null
                                ? entity.getCreatedAt().toOffsetDateTime()
                                : null);
    }
}
