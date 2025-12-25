package com.example.todo.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CommentTest {

    @Test
    void testEqualsAndHashCode() {
        Comment c1 = Comment.builder().id(1L).text("foo").build();
        Comment c2 = Comment.builder().id(1L).text("foo").build();
        Comment c3 = Comment.builder().id(2L).text("bar").build();
        Comment cNull = Comment.builder().build();
        Comment cNull2 = Comment.builder().build();

        assertEquals(c1, c1); // Same object
        assertEquals(c1, c2); // Same values
        assertNotEquals(c1, c3); // Different values
        assertNotEquals(c1, null);
        assertNotEquals(c1, "not a comment");
        
        assertEquals(cNull, cNull2); // Both null fields
        assertNotEquals(c1, cNull); // One null, one not
        
        Comment cText1 = Comment.builder().text("a").build();
        Comment cText2 = Comment.builder().text("b").build();
        assertNotEquals(cText1, cText2);

        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1.hashCode(), c3.hashCode());
    }

    @Test
    void testToString() {
        Comment c = Comment.builder().id(1L).text("greet").build();
        assertTrue(c.toString().contains("greet"));
    }

    @Test
    void testLifecycleMethods() {
        Comment c = new Comment();
        c.onCreate();
        assertNotNull(c.getCreatedAt());
        assertNotNull(c.getUpdatedAt());
        
        LocalDateTime before = c.getUpdatedAt();
        c.onUpdate();
        assertNotNull(c.getUpdatedAt());
    }
}
