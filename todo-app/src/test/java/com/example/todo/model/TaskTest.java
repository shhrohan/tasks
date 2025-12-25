package com.example.todo.model;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void testGettersAndSetters() {
        Task t = new Task();
        t.setId(1L);
        t.setName("Task");
        t.setStatus(TaskStatus.TODO);
        t.setTags("[\"tag\"]");
        t.setPosition(2);
        
        SwimLane s = new SwimLane();
        t.setSwimLane(s);
        
        t.setComments(new ArrayList<>());

        assertEquals(1L, t.getId());
        assertEquals("Task", t.getName());
        assertEquals(TaskStatus.TODO, t.getStatus());
        assertEquals("[\"tag\"]", t.getTags());
        assertEquals(2, t.getPosition());
        assertEquals(s, t.getSwimLane());
        assertNotNull(t.getComments());
    }

    @Test
    void testBuilder() {
        Task t = Task.builder()
                .id(1L)
                .name("Task")
                .status(TaskStatus.IN_PROGRESS)
                .build();
        
        assertEquals(1L, t.getId());
        assertEquals("Task", t.getName());
        assertEquals(TaskStatus.IN_PROGRESS, t.getStatus());
    }
}
