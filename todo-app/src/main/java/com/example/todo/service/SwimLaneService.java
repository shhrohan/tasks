package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SwimLaneService {

    private final SwimLaneDAO swimLaneDAO;
    private final AsyncWriteService asyncWriteService;
    private final UserRepository userRepository;

    public SwimLaneService(SwimLaneDAO swimLaneDAO, AsyncWriteService asyncWriteService, 
                          UserRepository userRepository) {
        this.swimLaneDAO = swimLaneDAO;
        this.asyncWriteService = asyncWriteService;
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated user from SecurityContext.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public List<SwimLane> getAllSwimLanes() {
        User user = getCurrentUser();
        log.info("Fetching swimlanes for user: {} (id={})", user.getEmail(), user.getId());
        return swimLaneDAO.findByUserIdAndIsDeletedFalseOrderByPositionAsc(user.getId());
    }

    public List<SwimLane> getActiveSwimLanes() {
        User user = getCurrentUser();
        return swimLaneDAO.findByUserIdAndIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc(user.getId());
    }

    public List<SwimLane> getCompletedSwimLanes() {
        User user = getCurrentUser();
        return swimLaneDAO.findByUserIdAndIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc(user.getId());
    }

    public SwimLane createSwimLane(SwimLane swimLane) {
        User user = getCurrentUser();
        swimLane.setUser(user);
        
        // Set position to max + 1 for this user
        Integer maxPos = swimLaneDAO.findMaxPositionByUserId(user.getId());
        swimLane.setPosition(maxPos == null ? 0 : maxPos + 1);
        log.info("Creating new swimlane '{}' for user: {}", swimLane.getName(), user.getEmail());
        return swimLaneDAO.save(swimLane);
    }

    public SwimLane completeSwimLane(Long id) {
        User user = getCurrentUser();
        log.debug("Marking swimlane {} as completed", id);
        SwimLane swimLane = swimLaneDAO.findById(id)
                .orElseThrow(() -> {
                    log.error("SwimLane not found with id: {}", id);
                    return new RuntimeException("SwimLane not found");
                });
        
        // Security check: ensure user owns this swimlane
        if (swimLane.getUser() == null || !swimLane.getUser().getId().equals(user.getId())) {
            log.error("User {} attempted to complete swimlane {} owned by another user", user.getEmail(), id);
            throw new RuntimeException("Access denied: You don't own this swimlane");
        }
        
        swimLane.setIsCompleted(true);
        log.info("Delegating COMPLETE for swimlane {} to Async Service", id);
        asyncWriteService.saveSwimLane(swimLane);
        return swimLane;
    }

    public SwimLane uncompleteSwimLane(Long id) {
        User user = getCurrentUser();
        log.debug("Reactivating swimlane {}", id);
        SwimLane swimLane = swimLaneDAO.findById(id)
                .orElseThrow(() -> {
                    log.error("SwimLane not found with id: {}", id);
                    return new RuntimeException("SwimLane not found");
                });
        
        // Security check: ensure user owns this swimlane
        if (swimLane.getUser() == null || !swimLane.getUser().getId().equals(user.getId())) {
            log.error("User {} attempted to uncomplete swimlane {} owned by another user", user.getEmail(), id);
            throw new RuntimeException("Access denied: You don't own this swimlane");
        }
        
        swimLane.setIsCompleted(false);
        log.info("Delegating UNCOMPLETE for swimlane {} to Async Service", id);
        asyncWriteService.saveSwimLane(swimLane);
        return swimLane;
    }

    public void deleteSwimLane(Long id) {
        User user = getCurrentUser();
        log.debug("Soft deleting swimlane {}", id);
        SwimLane swimLane = swimLaneDAO.findById(id)
                .orElseThrow(() -> {
                    log.error("SwimLane not found with id: {}", id);
                    return new RuntimeException("SwimLane not found");
                });
        
        // Security check: ensure user owns this swimlane
        if (swimLane.getUser() == null || !swimLane.getUser().getId().equals(user.getId())) {
            log.error("User {} attempted to delete swimlane {} owned by another user", user.getEmail(), id);
            throw new RuntimeException("Access denied: You don't own this swimlane");
        }
        
        swimLane.setIsDeleted(true);
        log.info("Delegating DELETE for swimlane {} to Async Service", id);
        asyncWriteService.saveSwimLane(swimLane);
    }

    public void hardDeleteSwimLane(Long id) {
        swimLaneDAO.deleteById(id);
    }

    public void reorderSwimLanes(List<Long> orderedIds) {
        User user = getCurrentUser();
        List<SwimLane> lanes = swimLaneDAO.findAllById(orderedIds);
        
        // Filter to only lanes owned by current user
        lanes = lanes.stream()
                .filter(lane -> lane.getUser() != null && lane.getUser().getId().equals(user.getId()))
                .collect(java.util.stream.Collectors.toList());
        
        // Create a map for O(1) lookup
        java.util.Map<Long, SwimLane> laneMap = lanes.stream()
                .collect(java.util.stream.Collectors.toMap(SwimLane::getId, java.util.function.Function.identity()));

        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            SwimLane lane = laneMap.get(id);
            if (lane != null) {
                lane.setPosition(i);
            }
        }
        swimLaneDAO.saveAll(lanes);
    }
}

