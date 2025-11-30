package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.model.SwimLane;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SwimLaneService {

    private final SwimLaneDAO swimLaneDAO;

    public SwimLaneService(SwimLaneDAO swimLaneDAO) {
        this.swimLaneDAO = swimLaneDAO;
    }

    public List<SwimLane> getAllSwimLanes() {
        return swimLaneDAO.findByIsDeletedFalse();
    }

    public List<SwimLane> getActiveSwimLanes() {
        return swimLaneDAO.findByIsCompletedFalseAndIsDeletedFalse();
    }

    public List<SwimLane> getCompletedSwimLanes() {
        return swimLaneDAO.findByIsCompletedTrueAndIsDeletedFalse();
    }

    public SwimLane createSwimLane(SwimLane swimLane) {
        log.debug("Saving new swimlane: {}", swimLane);
        return swimLaneDAO.save(swimLane);
    }

    public SwimLane completeSwimLane(Long id) {
        log.debug("Marking swimlane {} as completed", id);
        SwimLane swimLane = swimLaneDAO.findById(id)
                .orElseThrow(() -> {
                    log.error("SwimLane not found with id: {}", id);
                    return new RuntimeException("SwimLane not found");
                });
        swimLane.setIsCompleted(true);
        return swimLaneDAO.save(swimLane);
    }

    public SwimLane uncompleteSwimLane(Long id) {
        log.debug("Reactivating swimlane {}", id);
        SwimLane swimLane = swimLaneDAO.findById(id)
                .orElseThrow(() -> {
                    log.error("SwimLane not found with id: {}", id);
                    return new RuntimeException("SwimLane not found");
                });
        swimLane.setIsCompleted(false);
        return swimLaneDAO.save(swimLane);
    }

    public void deleteSwimLane(Long id) {
        log.debug("Soft deleting swimlane {}", id);
        SwimLane swimLane = swimLaneDAO.findById(id)
                .orElseThrow(() -> {
                    log.error("SwimLane not found with id: {}", id);
                    return new RuntimeException("SwimLane not found");
                });
        swimLane.setIsDeleted(true);
        swimLaneDAO.save(swimLane);
    }

    public void hardDeleteSwimLane(Long id) {
        swimLaneDAO.deleteById(id);
    }
}
