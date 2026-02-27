package com.deathstar.vader.controller;

import com.deathstar.vader.domain.Todo;
import com.deathstar.vader.dto.EventMessage;
import com.deathstar.vader.repository.TodoRepository;
import com.deathstar.vader.service.SseBroadcasterService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Controller strictly implementing the contract defined in holocron/openapi.yaml. */
@RestController
@RequestMapping("/api/v1")
public class TodoController {

    private final TodoRepository todoRepository;
    private final SseBroadcasterService sseService;

    public TodoController(TodoRepository todoRepository, SseBroadcasterService sseService) {
        this.todoRepository = todoRepository;
        this.sseService = sseService;
    }

    @GetMapping("/todos")
    public List<Todo> getTodos() {
        return todoRepository.findAllByOrderByCreatedAtDesc();
    }

    // A simple internal DTO mapped to the TodoCreate schema in OpenAPI
    public record TodoCreateRequest(String title) {}

    @PostMapping("/todos")
    @ResponseStatus(HttpStatus.CREATED)
    public Todo createTodo(@RequestBody TodoCreateRequest request) {
        // 1. Mutate state (Imperative execution)
        Todo newTodo = new Todo(request.title());
        Todo savedTodo = todoRepository.save(newTodo);

        // 2. Fire and forget event (Event-Driven architecture)
        sseService.publishEvent(EventMessage.created(savedTodo));

        return savedTodo;
    }

    /** Produces the text/event-stream required for the frontend's EventSource. */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents() {
        return sseService.registerClient();
    }
}
