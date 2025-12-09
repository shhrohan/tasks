package com.example.todo;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
public class TaskCreateIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldCreateTask() throws Exception {
        Task task = Task.builder()
                .name("Integration Test Task")
                .status(TaskStatus.TODO)
                .build();

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Integration Test Task")))
                .andExpect(jsonPath("$.status", is("TODO")));
    }
}
