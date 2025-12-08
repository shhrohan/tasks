package com.example.todo.controller;

import com.example.todo.base.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SseControllerTest extends BaseIntegrationTest {

    @Test
    void stream_ShouldReturnSSEConnection() throws Exception {
        mockMvc.perform(get("/api/sse/stream"))
                .andExpect(status().isOk());
    }
}
