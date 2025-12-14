package com.example.todo.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SwimLane model to cover Lombok-generated builder/getter/setter branches.
 * Note: SwimLane uses @Getter/@Setter/@Builder (not @Data), so equals/hashCode are reference-based.
 */
class SwimLaneTest {

    @Test
    void builder_ShouldCreateSwimLane() {
        SwimLane lane = SwimLane.builder()
                .id(1L)
                .name("Test Lane")
                .isCompleted(false)
                .isDeleted(false)
                .position(5)
                .build();

        assertEquals(1L, lane.getId());
        assertEquals("Test Lane", lane.getName());
        assertFalse(lane.getIsCompleted());
        assertFalse(lane.getIsDeleted());
        assertEquals(5, lane.getPosition());
    }

    @Test
    void builder_ShouldUseDefaults() {
        SwimLane lane = SwimLane.builder()
                .name("Test")
                .build();

        assertNull(lane.getId());
        assertEquals("Test", lane.getName());
        assertFalse(lane.getIsCompleted()); // @Builder.Default
        assertFalse(lane.getIsDeleted());   // @Builder.Default
        assertNull(lane.getPosition());
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        SwimLane lane = new SwimLane();
        assertNull(lane.getId());
        assertNull(lane.getName());
    }

    @Test
    void allArgsConstructor_ShouldWork() {
        User user = new User(1L, "Test User", "test@example.com");
        SwimLane lane = new SwimLane(1L, "Test", true, false, 10, user);
        assertEquals(1L, lane.getId());
        assertEquals("Test", lane.getName());
        assertTrue(lane.getIsCompleted());
        assertFalse(lane.getIsDeleted());
        assertEquals(10, lane.getPosition());
        assertEquals(user, lane.getUser());
    }

    @Test
    void setters_ShouldWork() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        lane.setName("Test");
        lane.setIsCompleted(true);
        lane.setIsDeleted(false);
        lane.setPosition(5);

        assertEquals(1L, lane.getId());
        assertEquals("Test", lane.getName());
        assertTrue(lane.getIsCompleted());
        assertFalse(lane.getIsDeleted());
        assertEquals(5, lane.getPosition());
    }

    @Test
    void setters_ShouldHandleNullValues() {
        SwimLane lane = new SwimLane();
        lane.setId(null);
        lane.setName(null);
        lane.setIsCompleted(null);
        lane.setIsDeleted(null);
        lane.setPosition(null);

        assertNull(lane.getId());
        assertNull(lane.getName());
        assertNull(lane.getIsCompleted());
        assertNull(lane.getIsDeleted());
        assertNull(lane.getPosition());
    }

    @Test
    void builder_ShouldHandleNullValues() {
        SwimLane lane = SwimLane.builder()
                .id(null)
                .name(null)
                .isCompleted(null)
                .isDeleted(null)
                .position(null)
                .build();

        assertNull(lane.getId());
        assertNull(lane.getName());
        assertNull(lane.getIsCompleted());
        assertNull(lane.getIsDeleted());
        assertNull(lane.getPosition());
    }
}
