package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.SwimLane;
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
    private SwimLaneService swimLaneService;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private CacheWarmupService cacheWarmupService;

    private User testUser;
    private SwimLane testLane;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testLane = new SwimLane();
        testLane.setId(1L);
        testLane.setName("Test Lane");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setName("Test Task");
        testTask.setStatus(TaskStatus.TODO);
    }

    @Test
    void warmUpCaches_ShouldFetchAllUsers() {
        // Arrange
        when(userDAO.findAll()).thenReturn(Collections.singletonList(testUser));
        when(swimLaneService.getActiveSwimLanesForUser(1L)).thenReturn(Collections.emptyList());

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(userDAO).findAll();
    }

    @Test
    void warmUpCaches_ShouldFetchLanesForAllUsers() {
        // Arrange
        when(userDAO.findAll()).thenReturn(Collections.singletonList(testUser));
        when(swimLaneService.getActiveSwimLanesForUser(1L)).thenReturn(Collections.singletonList(testLane));
        when(taskService.getTasksBySwimLaneId(1L)).thenReturn(Collections.singletonList(testTask));

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(swimLaneService).getActiveSwimLanesForUser(1L);
    }

    @Test
    void warmUpCaches_ShouldFetchTasksForAllLanes() {
        // Arrange
        when(userDAO.findAll()).thenReturn(Collections.singletonList(testUser));
        when(swimLaneService.getActiveSwimLanesForUser(1L)).thenReturn(Collections.singletonList(testLane));
        when(taskService.getTasksBySwimLaneId(1L)).thenReturn(Collections.singletonList(testTask));

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(taskService).getTasksBySwimLaneId(1L);
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

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(userDAO).findAll();
        verifyNoInteractions(swimLaneService);
        verifyNoInteractions(taskService);
    }

    @Test
    void warmUpCaches_ShouldHandleMultipleUsersAndLanes() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@example.com");

        SwimLane lane2 = new SwimLane();
        lane2.setId(2L);
        lane2.setName("Lane 2");

        when(userDAO.findAll()).thenReturn(Arrays.asList(testUser, user2));
        when(swimLaneService.getActiveSwimLanesForUser(1L)).thenReturn(Collections.singletonList(testLane));
        when(swimLaneService.getActiveSwimLanesForUser(2L)).thenReturn(Collections.singletonList(lane2));
        when(taskService.getTasksBySwimLaneId(anyLong())).thenReturn(Collections.emptyList());

        // Act
        cacheWarmupService.warmUpCaches();

        // Assert
        verify(userDAO).findAll();
        verify(swimLaneService).getActiveSwimLanesForUser(1L);
        verify(swimLaneService).getActiveSwimLanesForUser(2L);
    }
}
