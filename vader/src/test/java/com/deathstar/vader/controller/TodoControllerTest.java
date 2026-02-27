package com.deathstar.vader.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deathstar.vader.domain.Todo;
import com.deathstar.vader.dto.EventMessage;
import com.deathstar.vader.repository.TodoRepository;
import com.deathstar.vader.service.SseBroadcasterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 針對 HTTP 邊界的切片測試。 專注於驗證路由、狀態碼與 JSON 序列化，將業務邏輯與資料庫依賴 Mock 掉。 */
@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    // 在 Spring Boot 3.4 中使用 @MockitoBean 替代 @MockBean
    @MockitoBean private TodoRepository repository;

    @MockitoBean private SseBroadcasterService sseService;

    @Test
    void shouldReturnAllTodos() throws Exception {
        var todo = new Todo("Destroy Alderaan");
        // FIX: The correct standard Java API is randomUUID()
        todo.setId(UUID.randomUUID());
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(todo));

        mockMvc.perform(get("/api/v1/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Destroy Alderaan"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void shouldCreateTodoAndPublishEvent() throws Exception {
        var inputTodo = new Todo("Build Death Star");
        var savedTodo = new Todo("Build Death Star");
        // FIX: The correct standard Java API is randomUUID()
        savedTodo.setId(UUID.randomUUID());

        when(repository.save(any(Todo.class))).thenReturn(savedTodo);

        mockMvc.perform(
                        post("/api/v1/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(inputTodo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Build Death Star"));

        // FIX: Explicitly specify the boundary class to prevent Mockito type inference degradation
        verify(sseService).publishEvent(any(EventMessage.class));
    }
}
