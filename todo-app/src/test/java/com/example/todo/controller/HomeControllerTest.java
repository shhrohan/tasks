package com.example.todo.controller;

import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.service.SwimLaneService;
import com.example.todo.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private SwimLaneService swimLaneService;

    @Mock
    private TaskService taskService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Model model;

    @InjectMocks
    private HomeController homeController;

    private SwimLane testLane;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testLane = new SwimLane();
        testLane.setId(1L);
        testLane.setName("Test Lane");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setName("Test Task");
        testTask.setStatus(TaskStatus.TODO);
    }

    @Test
    void index_ShouldReturnIndexView() throws Exception {
        // Arrange
        when(swimLaneService.getActiveSwimLanes()).thenReturn(Collections.emptyList());
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        String viewName = homeController.index(model);

        // Assert
        assertEquals("index", viewName);
    }

    @Test
    void index_ShouldAddInitialDataJsonToModel() throws Exception {
        // Arrange
        List<SwimLane> lanes = Arrays.asList(testLane);
        List<Task> tasks = Arrays.asList(testTask);
        String expectedJson = "{\"lanes\":[],\"tasks\":[]}";

        when(swimLaneService.getActiveSwimLanes()).thenReturn(lanes);
        when(taskService.getAllTasks()).thenReturn(tasks);
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);

        // Act
        homeController.index(model);

        // Assert
        verify(model).addAttribute(eq("initialDataJson"), anyString());
    }

    @Test
    void index_ShouldFetchActiveSwimLanes() throws Exception {
        // Arrange
        when(swimLaneService.getActiveSwimLanes()).thenReturn(Collections.emptyList());
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        homeController.index(model);

        // Assert
        verify(swimLaneService).getActiveSwimLanes();
    }

    @Test
    void index_ShouldFetchAllTasks() throws Exception {
        // Arrange
        when(swimLaneService.getActiveSwimLanes()).thenReturn(Collections.emptyList());
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        homeController.index(model);

        // Assert
        verify(taskService).getAllTasks();
    }

    @Test
    void index_ShouldSerializeDataToJson() throws Exception {
        // Arrange
        List<SwimLane> lanes = Arrays.asList(testLane);
        List<Task> tasks = Arrays.asList(testTask);

        when(swimLaneService.getActiveSwimLanes()).thenReturn(lanes);
        when(taskService.getAllTasks()).thenReturn(tasks);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"lanes\":[{\"id\":1}],\"tasks\":[{\"id\":1}]}");

        // Act
        homeController.index(model);

        // Assert
        verify(objectMapper).writeValueAsString(any());
    }
}
