package com.example.todo.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testGettersAndSetters() {
        User u = new User();
        u.setId(1L);
        u.setName("Rohan");
        u.setEmail("rohan@test.com");
        u.setPasswordHash("hash");
        LocalDateTime now = LocalDateTime.now();
        u.setCreatedAt(now);

        assertEquals(1L, u.getId());
        assertEquals("Rohan", u.getName());
        assertEquals("rohan@test.com", u.getEmail());
        assertEquals("hash", u.getPasswordHash());
        assertEquals(now, u.getCreatedAt());
    }

    @Test
    void testBuilder() {
        User u = User.builder()
                .id(1L)
                .name("Rohan")
                .email("rohan@test.com")
                .build();
        
        assertEquals(1L, u.getId());
        assertEquals("Rohan", u.getName());
        assertEquals("rohan@test.com", u.getEmail());
    }
}
