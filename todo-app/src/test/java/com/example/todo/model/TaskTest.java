package com.example.todo.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Task model to cover Lombok-generated builder/getter/setter branches.
 * Note: Task uses @Getter/@Setter/@Builder (not @Data), so equals/hashCode are reference-based.
 */
class TaskTest {

    @Test
    void builder_ShouldCreateTask() {
        SwimLane lane = SwimLane.builder().id(1L).name("Lane").build();
        List<Comment> comments = new ArrayList<>();
        comments.add(Comment.builder().id(1L).text("comment").build());

        Task task = Task.builder()
                .id(1L)
                .name("Test Task")
                .status(TaskStatus.TODO)
                .comments(comments)
                .tags("[\"tag\"]")
                .position(5)
                .swimLane(lane)
                .build();

        assertEquals(1L, task.getId());
        assertEquals("Test Task", task.getName());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertEquals(1, task.getComments().size());
        assertEquals("[\"tag\"]", task.getTags());
        assertEquals(5, task.getPosition());
        assertEquals(lane, task.getSwimLane());
    }

    @Test
    void builder_ShouldHandleNulls() {
        Task task = Task.builder()
                .id(null)
                .name(null)
                .status(null)
                .comments(null)
                .tags(null)
                .position(null)
                .swimLane(null)
                .build();

        assertNull(task.getId());
        assertNull(task.getName());
        assertNull(task.getStatus());
        assertNull(task.getComments());
        assertNull(task.getTags());
        assertNull(task.getPosition());
        assertNull(task.getSwimLane());
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        Task task = new Task();
        assertNull(task.getId());
        assertNull(task.getName());
        assertNull(task.getStatus());
    }

    @Test
    void allArgsConstructor_ShouldWork() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        List<Comment> comments = new ArrayList<>();

        Task task = new Task(1L, "Test", TaskStatus.DONE, comments, "[]", lane, 10);
        assertEquals(1L, task.getId());
        assertEquals("Test", task.getName());
        assertEquals(TaskStatus.DONE, task.getStatus());
        assertEquals(comments, task.getComments());
        assertEquals("[]", task.getTags());
        assertEquals(lane, task.getSwimLane());
        assertEquals(10, task.getPosition());
    }

    @Test
    void setters_ShouldWork() {
        Task task = new Task();
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        List<Comment> comments = new ArrayList<>();

        task.setId(1L);
        task.setName("Test");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setComments(comments);
        task.setTags("[\"tag\"]");
        task.setPosition(5);
        task.setSwimLane(lane);

        assertEquals(1L, task.getId());
        assertEquals("Test", task.getName());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(comments, task.getComments());
        assertEquals("[\"tag\"]", task.getTags());
        assertEquals(5, task.getPosition());
        assertEquals(lane, task.getSwimLane());
    }

    @Test
    void setters_ShouldHandleNullValues() {
        Task task = new Task();
        task.setId(null);
        task.setName(null);
        task.setStatus(null);
        task.setComments(null);
        task.setTags(null);
        task.setPosition(null);
        task.setSwimLane(null);

        assertNull(task.getId());
        assertNull(task.getName());
        assertNull(task.getStatus());
        assertNull(task.getComments());
        assertNull(task.getTags());
        assertNull(task.getPosition());
        assertNull(task.getSwimLane());
    }

    @Test
    void allStatusValues_ShouldBeValid() {
        for (TaskStatus status : TaskStatus.values()) {
            Task task = Task.builder().id(1L).name("Test").status(status).build();
            assertEquals(status, task.getStatus());
        }
    }

    @Test
    void statusEnum_ShouldHaveAllValues() {
        TaskStatus[] values = TaskStatus.values();
        assertEquals(5, values.length);
        assertEquals(TaskStatus.TODO, TaskStatus.valueOf("TODO"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.valueOf("IN_PROGRESS"));
        assertEquals(TaskStatus.DONE, TaskStatus.valueOf("DONE"));
        assertEquals(TaskStatus.BLOCKED, TaskStatus.valueOf("BLOCKED"));
        assertEquals(TaskStatus.DEFERRED, TaskStatus.valueOf("DEFERRED"));
    }
}
