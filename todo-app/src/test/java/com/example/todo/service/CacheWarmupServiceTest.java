package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheWarmupServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private CacheWarmupService cacheWarmupService;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setName("Test Task");
        testTask.setStatus(TaskStatus.TODO);
    }

    @Test
    void warmUpCaches_ShouldFetchAllUsers() {
        // Arrange
        when(userDAO.findAll()).thenReturn(Collections.singletonList(testUser));
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(userDAO).findAll();
    }

    @Test
    void warmUpCaches_ShouldFetchAllTasks() {
        // Arrange
        when(userDAO.findAll()).thenReturn(Collections.singletonList(testUser));
        when(taskService.getAllTasks()).thenReturn(Arrays.asList(testTask));

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(taskService).getAllTasks();
    }

    @Test
    void warmUpCaches_ShouldHandleException() {
        // Arrange - Simulate exception during user fetch
        when(userDAO.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act - Should not throw, exception is caught and logged
        cacheWarmupService.warmUpCaches();

        // Assert - Method completes without exception
        verify(userDAO).findAll();
    }

    @Test
    void warmUpCaches_ShouldHandleEmptyUserList() {
        // Arrange
        when(userDAO.findAll()).thenReturn(Collections.emptyList());
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(userDAO).findAll();
        verify(taskService).getAllTasks();
    }

    @Test
    void warmUpCaches_ShouldHandleMultipleUsers() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@example.com");

        when(userDAO.findAll()).thenReturn(Arrays.asList(testUser, user2));
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(userDAO).findAll();
        verify(taskService).getAllTasks();
    }
}
