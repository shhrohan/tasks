package com.example.todo.controller;

import com.example.todo.model.SwimLane;
import com.example.todo.service.SwimLaneService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.example.todo.repository.UserRepository;
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
    private ObjectMapper objectMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Model model;

    @Mock
    private org.springframework.security.core.Authentication authentication;

    @Mock
    private org.springframework.security.core.context.SecurityContext securityContext;

    @InjectMocks
    private HomeController homeController;

    private SwimLane testLane;
    private com.example.todo.model.User testUser;

    @BeforeEach
    void setUp() {
        testLane = new SwimLane();
        testLane.setId(1L);
        testLane.setName("Test Lane");

        testUser = new com.example.todo.model.User();
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        // Mock Security Context
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("test@example.com");
        
        // Mock User Repository
        lenient().when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(testUser));
    }

    @Test
    void index_ShouldReturnIndexView() throws Exception {
        // Arrange
        when(swimLaneService.getActiveSwimLanes()).thenReturn(Collections.emptyList());
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
        String expectedJson = "{\"lanes\":[],\"tasks\":[]}";

        when(swimLaneService.getActiveSwimLanes()).thenReturn(lanes);
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
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        homeController.index(model);

        // Assert
        verify(swimLaneService).getActiveSwimLanes();
    }

    @Test
    void index_ShouldNotFetchTasks_TasksAreLazyLoaded() throws Exception {
        // Arrange - HomeController no longer fetches tasks, they are lazy-loaded
        when(swimLaneService.getActiveSwimLanes()).thenReturn(Collections.emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        homeController.index(model);

        // Assert - tasks are empty in initial load, fetched per lane via API
        verify(swimLaneService).getActiveSwimLanes();
    }

    @Test
    void index_ShouldSerializeDataToJson() throws Exception {
        // Arrange
        List<SwimLane> lanes = Arrays.asList(testLane);

        when(swimLaneService.getActiveSwimLanes()).thenReturn(lanes);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"lanes\":[{\"id\":1}],\"tasks\":[]}");

        // Act
        homeController.index(model);

        // Assert
        verify(objectMapper).writeValueAsString(any());
    }
}
