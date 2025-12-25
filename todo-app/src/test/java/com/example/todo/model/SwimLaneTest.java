package com.example.todo.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwimLaneTest {

    @Test
    void testGettersAndSetters() {
        SwimLane s = new SwimLane();
        s.setId(1L);
        s.setName("Lane");
        s.setIsCompleted(true);
        s.setIsDeleted(true);
        s.setPosition(5);
        User u = new User();
        s.setUser(u);

        assertEquals(1L, s.getId());
        assertEquals("Lane", s.getName());
        assertTrue(s.getIsCompleted());
        assertTrue(s.getIsDeleted());
        assertEquals(5, s.getPosition());
        assertEquals(u, s.getUser());
    }

    @Test
    void testBuilder() {
        SwimLane s = SwimLane.builder()
                .id(1L)
                .name("Lane")
                .isCompleted(false)
                .isDeleted(false)
                .position(1)
                .build();
        
        assertEquals(1L, s.getId());
        assertEquals("Lane", s.getName());
    }
}
