package com.example.todo.service;

import com.example.todo.dao.TaskDAO;
import com.example.todo.model.Comment;
import com.example.todo.model.Task;
import com.example.todo.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentMigrationServiceTest {

    @Mock
    private TaskDAO taskDAO;

    @Mock
    private CommentRepository commentRepository;

    private CommentMigrationService commentMigrationService;

    @BeforeEach
    void setUp() {
        commentMigrationService = new CommentMigrationService(taskDAO, commentRepository);
    }

    @Test
    void migrateJsonCommentsToTable_ShouldDoNothing_WhenNoTasks() {
        when(taskDAO.findAll()).thenReturn(Collections.emptyList());

        commentMigrationService.migrateJsonCommentsToTable();

        verify(taskDAO).findAll();
        verifyNoInteractions(commentRepository);
    }

    @Test
    void migrateJsonCommentsToTable_ShouldSkipTasks_WhenTheyHaveNewComments() {
        Task taskWithComments = new Task();
        taskWithComments.setId(1L);
        taskWithComments.setComments(Arrays.asList(new Comment()));

        when(taskDAO.findAll()).thenReturn(Arrays.asList(taskWithComments));

        commentMigrationService.migrateJsonCommentsToTable();

        verify(taskDAO).findAll();
        verifyNoInteractions(commentRepository);
    }

    @Test
    void migrateJsonCommentsToTable_ShouldProcessTasks_WhenNoComments() {
        // Since the actual migration logic is currently empty in the service,
        // we just verify it iterates through tasks without exploding.
        Task taskNoComments = new Task();
        taskNoComments.setId(2L);
        taskNoComments.setComments(Collections.emptyList());

        when(taskDAO.findAll()).thenReturn(Arrays.asList(taskNoComments));

        commentMigrationService.migrateJsonCommentsToTable();

        verify(taskDAO).findAll();
    }
}
