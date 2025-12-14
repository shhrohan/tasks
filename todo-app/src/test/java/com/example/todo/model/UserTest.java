package com.example.todo.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for User model to cover Lombok-generated builder/getter/setter.
 */
class UserTest {

    @Test
    void builder_ShouldCreateUser() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        assertEquals(1L, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void builder_ShouldHandleNulls() {
        User user = User.builder()
                .id(null)
                .name(null)
                .email(null)
                .build();

        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
    }

    @Test
    void allArgsConstructor_ShouldWork() {
        User user = new User(1L, "Test", "test@example.com");
        assertEquals(1L, user.getId());
        assertEquals("Test", user.getName());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void setters_ShouldWork() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        assertEquals(1L, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void setters_ShouldHandleNullValues() {
        User user = new User();
        user.setId(null);
        user.setName(null);
        user.setEmail(null);

        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
    }
}
