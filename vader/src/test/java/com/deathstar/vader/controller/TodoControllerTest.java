package com.deathstar.vader.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deathstar.vader.domain.Todo;
import com.deathstar.vader.dto.EventMessage;
import com.deathstar.vader.repository.TodoRepository;
import com.deathstar.vader.security.DistributedRevocationService;
import com.deathstar.vader.security.JwtProvider;
import com.deathstar.vader.service.SseBroadcasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for the HTTP boundary. Focuses on validating routing, status codes, and JSON
 * serialization, mocking out business logic and database dependencies.
 */
@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    // Use @MockitoBean instead of @MockBean in Spring Boot 3.4
    @MockitoBean private TodoRepository repository;

    @MockitoBean private SseBroadcasterService sseService;

    // --- FIX: Satisfy the dependency injection black hole for the Security Filter ---
    @MockitoBean private JwtProvider jwtProvider;

    @MockitoBean private DistributedRevocationService revocationService;

    @Test
    @WithMockUser(
            username = "skywalker",
            roles = {"USER"}) // FIX: Bypass 401 validation, establish virtual SecurityContext
    void shouldReturnAllTodos() throws Exception {
        var todo = new Todo("Destroy Alderaan");
        // FIX: The correct standard Java API is randomUUID()
        todo.setId(UUID.randomUUID());
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(todo));

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Destroy Alderaan"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    @WithMockUser(
            username = "skywalker",
            roles = {"USER"}) // FIX: Bypass 401 validation
    void shouldCreateTodoAndPublishEvent() throws Exception {
        var inputTodo = new Todo("Build Death Star");
        var savedTodo = new Todo("Build Death Star");
        // FIX: The correct standard Java API is randomUUID()
        savedTodo.setId(UUID.randomUUID());

        when(repository.save(any(Todo.class))).thenReturn(savedTodo);

        mockMvc.perform(
                        post("/todos")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(inputTodo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Build Death Star"));

        // FIX: Explicitly specify the boundary class to prevent Mockito type inference degradation
        verify(sseService).publishEvent(any(EventMessage.class));
    }
}
