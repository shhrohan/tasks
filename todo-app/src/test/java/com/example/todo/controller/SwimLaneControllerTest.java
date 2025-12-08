package com.example.todo.controller;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.SwimLane;
import com.example.todo.repository.SwimLaneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SwimLaneControllerTest extends BaseIntegrationTest {

    @Autowired
    private SwimLaneRepository swimLaneRepository;

    @Test
    void getAllSwimLanes_ShouldReturnList() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setName("Lane 1");
        swimLaneRepository.save(lane);

        mockMvc.perform(get("/api/swimlanes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lane 1"));
    }

    @Test
    void createSwimLane_ShouldReturnCreatedLane() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setName("New Lane");

        mockMvc.perform(post("/api/swimlanes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lane)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Lane"));

        List<SwimLane> lanes = swimLaneRepository.findAll();
        assertTrue(lanes.size() >= 1);
        assertTrue(lanes.stream().anyMatch(l -> "New Lane".equals(l.getName())));
    }

    @Test
    void completeSwimLane_ShouldReturnCompletedLane() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setName("To Complete");
        lane.setIsCompleted(false);
        lane = swimLaneRepository.save(lane);

        mockMvc.perform(patch("/api/swimlanes/{id}/complete", lane.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));

        SwimLane updatedLane = swimLaneRepository.findById(lane.getId()).orElseThrow();
        assertTrue(updatedLane.getIsCompleted());
    }

    @Test
    void getActiveSwimLanes_ShouldReturnActiveLanes() throws Exception {
        SwimLane activeLane = new SwimLane();
        activeLane.setName("Active Test Lane");
        activeLane.setIsCompleted(false);
        swimLaneRepository.save(activeLane);

        SwimLane completedLane = new SwimLane();
        completedLane.setName("Completed Test Lane");
        completedLane.setIsCompleted(true);
        swimLaneRepository.save(completedLane);

        mockMvc.perform(get("/api/swimlanes/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Active Test Lane')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Completed Test Lane')]").doesNotExist());
    }

    @Test
    void getCompletedSwimLanes_ShouldReturnCompletedLanes() throws Exception {
        SwimLane activeLane = new SwimLane();
        activeLane.setName("Active Lane For Completed Test");
        activeLane.setIsCompleted(false);
        swimLaneRepository.save(activeLane);

        SwimLane completedLane = new SwimLane();
        completedLane.setName("Completed Lane For Test");
        completedLane.setIsCompleted(true);
        swimLaneRepository.save(completedLane);

        mockMvc.perform(get("/api/swimlanes/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Completed Lane For Test')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Active Lane For Completed Test')]").doesNotExist());
    }

    @Test
    void uncompleteSwimLane_ShouldReturnActiveLane() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setName("To Uncomplete");
        lane.setIsCompleted(true);
        lane = swimLaneRepository.save(lane);

        mockMvc.perform(patch("/api/swimlanes/{id}/uncomplete", lane.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(false));

        SwimLane updatedLane = swimLaneRepository.findById(lane.getId()).orElseThrow();
        assertFalse(updatedLane.getIsCompleted());
    }

    @Test
    void deleteSwimLane_ShouldReturnOk() throws Exception {
        SwimLane lane = new SwimLane();
        lane.setName("To Delete");
        lane = swimLaneRepository.save(lane);

        mockMvc.perform(delete("/api/swimlanes/{id}", lane.getId()))
                .andExpect(status().isOk());

        SwimLane deletedLane = swimLaneRepository.findById(lane.getId()).orElseThrow();
        assertTrue(deletedLane.getIsDeleted());
    }
}
