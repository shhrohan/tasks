package com.example.todo.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Comment model (now a JPA entity with Long ID).
 */
class CommentTest {

    @Test
    void equals_ShouldReturnTrue_WhenSameObject() {
        Comment comment = createComment(1L, "Test");
        assertEquals(comment, comment);
    }

    @Test
    void equals_ShouldReturnFalse_WhenNull() {
        Comment comment = createComment(1L, "Test");
        assertNotEquals(null, comment);
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferentClass() {
        Comment comment = createComment(1L, "Test");
        assertNotEquals("not a comment", comment);
    }

    @Test
    void equals_ShouldReturnTrue_WhenAllFieldsEqual() {
        LocalDateTime now = LocalDateTime.now();
        Comment c1 = Comment.builder()
                .id(1L)
                .text("Test")
                .createdAt(now)
                .updatedAt(now)
                .build();
        Comment c2 = Comment.builder()
                .id(1L)
                .text("Test")
                .createdAt(now)
                .updatedAt(now)
                .build();
        assertEquals(c1, c2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenIdDifferent() {
        LocalDateTime now = LocalDateTime.now();
        Comment c1 = Comment.builder().id(1L).text("Test").createdAt(now).updatedAt(now).build();
        Comment c2 = Comment.builder().id(2L).text("Test").createdAt(now).updatedAt(now).build();
        assertNotEquals(c1, c2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenTextDifferent() {
        LocalDateTime now = LocalDateTime.now();
        Comment c1 = Comment.builder().id(1L).text("Test1").createdAt(now).updatedAt(now).build();
        Comment c2 = Comment.builder().id(1L).text("Test2").createdAt(now).updatedAt(now).build();
        assertNotEquals(c1, c2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenCreatedAtDifferent() {
        Comment c1 = Comment.builder().id(1L).text("Test")
                .createdAt(LocalDateTime.of(2023, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.now()).build();
        Comment c2 = Comment.builder().id(1L).text("Test")
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.now()).build();
        assertNotEquals(c1, c2);
    }

    @Test
    void equals_ShouldReturnFalse_WhenUpdatedAtDifferent() {
        LocalDateTime created = LocalDateTime.now();
        Comment c1 = Comment.builder().id(1L).text("Test")
                .createdAt(created)
                .updatedAt(LocalDateTime.of(2023, 1, 1, 10, 0)).build();
        Comment c2 = Comment.builder().id(1L).text("Test")
                .createdAt(created)
                .updatedAt(LocalDateTime.of(2024, 1, 1, 10, 0)).build();
        assertNotEquals(c1, c2);
    }

    @Test
    void equals_ShouldHandleNullId() {
        Comment c1 = Comment.builder().id(null).text("Test").build();
        Comment c2 = Comment.builder().id(null).text("Test").build();
        assertEquals(c1, c2);

        Comment c3 = Comment.builder().id(1L).text("Test").build();
        assertNotEquals(c1, c3);
    }

    @Test
    void equals_ShouldHandleNullText() {
        Comment c1 = Comment.builder().id(1L).text(null).build();
        Comment c2 = Comment.builder().id(1L).text(null).build();
        assertEquals(c1, c2);

        Comment c3 = Comment.builder().id(1L).text("Test").build();
        assertNotEquals(c1, c3);
    }

    @Test
    void equals_ShouldHandleNullDates() {
        Comment c1 = Comment.builder().id(1L).text("Test").createdAt(null).updatedAt(null).build();
        Comment c2 = Comment.builder().id(1L).text("Test").createdAt(null).updatedAt(null).build();
        assertEquals(c1, c2);
    }

    @Test
    void hashCode_ShouldBeConsistent() {
        Comment comment = createComment(1L, "Test");
        int hashCode1 = comment.hashCode();
        int hashCode2 = comment.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void hashCode_ShouldBeEqual_WhenObjectsEqual() {
        LocalDateTime now = LocalDateTime.now();
        Comment c1 = Comment.builder().id(1L).text("Test").createdAt(now).updatedAt(now).build();
        Comment c2 = Comment.builder().id(1L).text("Test").createdAt(now).updatedAt(now).build();
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void hashCode_ShouldHandleNullFields() {
        Comment c1 = Comment.builder().id(null).text(null).createdAt(null).updatedAt(null).build();
        assertDoesNotThrow(() -> c1.hashCode());
    }

    @Test
    void toString_ShouldReturnString() {
        Comment comment = createComment(1L, "Test");
        assertNotNull(comment.toString());
        assertTrue(comment.toString().contains("Test"));
    }

    @Test
    void builder_ShouldCreateComment() {
        Comment comment = Comment.builder()
                .id(1L)
                .text("test-text")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertEquals(1L, comment.getId());
        assertEquals("test-text", comment.getText());
        assertNotNull(comment.getCreatedAt());
        assertNotNull(comment.getUpdatedAt());
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        Comment comment = new Comment();
        assertNull(comment.getId());
        assertNull(comment.getText());
    }

    @Test
    void allArgsConstructor_ShouldWork() {
        LocalDateTime now = LocalDateTime.now();
        Task task = new Task();
        Comment comment = new Comment(1L, "text", now, now, task);
        assertEquals(1L, comment.getId());
        assertEquals("text", comment.getText());
        assertEquals(now, comment.getCreatedAt());
        assertEquals(now, comment.getUpdatedAt());
        assertEquals(task, comment.getTask());
    }

    @Test
    void setters_ShouldWork() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("new-text");
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        assertEquals(1L, comment.getId());
        assertEquals("new-text", comment.getText());
        assertEquals(now, comment.getCreatedAt());
        assertEquals(now, comment.getUpdatedAt());
    }

    private Comment createComment(Long id, String text) {
        return Comment.builder()
                .id(id)
                .text(text)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
