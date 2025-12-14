package com.example.todo.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for User model to cover Lombok-generated builder/getter/setter.
 */
class UserTest {

    @Test
    void builder_ShouldCreateUser() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .createdAt(now)
                .build();

        assertEquals(1L, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedpassword", user.getPasswordHash());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void builder_ShouldHandleNulls() {
        User user = User.builder()
                .id(null)
                .name(null)
                .email(null)
                .passwordHash(null)
                .createdAt(null)
                .build();

        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getCreatedAt());
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
    }

    @Test
    void allArgsConstructor_ShouldWork() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "Test", "test@example.com", "hash", now);
        assertEquals(1L, user.getId());
        assertEquals("Test", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void setters_ShouldWork() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpw");
        user.setCreatedAt(now);

        assertEquals(1L, user.getId());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedpw", user.getPasswordHash());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void setters_ShouldHandleNullValues() {
        User user = new User();
        user.setId(null);
        user.setName(null);
        user.setEmail(null);
        user.setPasswordHash(null);
        user.setCreatedAt(null);

        assertNull(user.getId());
        assertNull(user.getName());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getCreatedAt());
    }

    @Test
    void onCreate_ShouldSetCreatedAt_WhenNull() throws Exception {
        User user = new User();
        assertNull(user.getCreatedAt());
        
        // Call the @PrePersist method directly
        java.lang.reflect.Method onCreateMethod = User.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(user);
        
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void onCreate_ShouldNotOverwriteCreatedAt_WhenAlreadySet() throws Exception {
        LocalDateTime existingTime = LocalDateTime.of(2023, 1, 1, 12, 0);
        User user = new User();
        user.setCreatedAt(existingTime);
        
        // Call the @PrePersist method directly
        java.lang.reflect.Method onCreateMethod = User.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(user);
        
        // Should not overwrite existing value
        assertEquals(existingTime, user.getCreatedAt());
    }
}


