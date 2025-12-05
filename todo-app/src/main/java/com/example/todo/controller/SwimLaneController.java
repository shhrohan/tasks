package com.example.todo.controller;

import com.example.todo.model.SwimLane;
import com.example.todo.service.SwimLaneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/swimlanes")
@Log4j2
public class SwimLaneController {

    private final SwimLaneService swimLaneService;

    public SwimLaneController(SwimLaneService swimLaneService) {
        this.swimLaneService = swimLaneService;
    }

    @GetMapping
    public List<SwimLane> getAllSwimLanes() {
        log.info("Fetching all swimlanes");
        return swimLaneService.getAllSwimLanes();
    }

    @GetMapping("/active")
    public List<SwimLane> getActiveSwimLanes() {
        return swimLaneService.getActiveSwimLanes();
    }

    @GetMapping("/completed")
    public List<SwimLane> getCompletedSwimLanes() {
        return swimLaneService.getCompletedSwimLanes();
    }

    @PostMapping
    public SwimLane createSwimLane(@RequestBody SwimLane swimLane) {
        log.info("Creating new swimlane: {}", swimLane.getName());
        return swimLaneService.createSwimLane(swimLane);
    }

    @PatchMapping("/{id}/complete")
    public SwimLane completeSwimLane(@PathVariable Long id) {
        log.info("Completing swimlane with id: {}", id);
        return swimLaneService.completeSwimLane(id);
    }

    @PatchMapping("/{id}/uncomplete")
    public SwimLane uncompleteSwimLane(@PathVariable Long id) {
        log.info("Reactivating swimlane with id: {}", id);
        return swimLaneService.uncompleteSwimLane(id);
    }

    @PatchMapping("/reorder")
    public void reorderSwimLanes(@RequestBody List<Long> orderedIds) {
        log.info("Reordering swimlanes: {}", orderedIds);
        swimLaneService.reorderSwimLanes(orderedIds);
    }

    @DeleteMapping("/{id}")
    public void deleteSwimLane(@PathVariable Long id) {
        log.info("Deleting swimlane with id: {}", id);
        swimLaneService.deleteSwimLane(id);
    }
}
