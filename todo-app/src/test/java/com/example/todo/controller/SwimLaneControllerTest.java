package com.example.todo.controller;

import com.example.todo.model.SwimLane;
import com.example.todo.service.SwimLaneService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SwimLaneController.class)
class SwimLaneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SwimLaneService swimLaneService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllSwimLanes_ShouldReturnList() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        lane.setName("Lane 1");

        when(swimLaneService.getAllSwimLanes()).thenReturn(Arrays.asList(lane));

        mockMvc.perform(get("/api/swimlanes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lane 1"));
    }

    @Test
    void createSwimLane_ShouldReturnCreatedLane() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setName("New Lane");

        when(swimLaneService.createSwimLane(any(SwimLane.class))).thenReturn(lane);

        mockMvc.perform(post("/api/swimlanes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lane)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Lane"));
    }

    @Test
    void completeSwimLane_ShouldReturnCompletedLane() throws Exception {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setIsCompleted(true);

        when(swimLaneService.completeSwimLane(laneId)).thenReturn(lane);

        mockMvc.perform(patch("/api/swimlanes/{id}/complete", laneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));
    }
}
