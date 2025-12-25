package com.example.todo.controller;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void updateUser_ShouldUpdateName() throws Exception {
        mockMvc.perform(put("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"New Name\""))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals("New Name", updated.getName());
    }

    @Test
    void updateUser_ShouldHandleUnquotedName() throws Exception {
        mockMvc.perform(put("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("Plain Name"))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals("Plain Name", updated.getName());
    }

    @Test
    void updateUser_ShouldHandlePartialQuotes() throws Exception {
        // Only starts with quote
        mockMvc.perform(put("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Only Start"))
                .andExpect(status().isOk());

        // Only ends with quote
        mockMvc.perform(put("/api/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("Only End\""))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_ShouldReturnBadRequest_WhenUserNotFound() throws Exception {
        // Authenticate as a non-existent user
        mockMvc.perform(put("/api/user")
                .with(user("nonexistent@deleted.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"New Name\""))
                .andExpect(status().isBadRequest());
    }
}
