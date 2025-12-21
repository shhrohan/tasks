package com.example.todo.service;

import com.example.todo.annotation.Idempotent;
import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Cacheable(value = "lanes", key = "#root.target.currentUserId")
    public List<SwimLane> getAllSwimLanes() {
        long start = System.currentTimeMillis();
        User user = getCurrentUser();
        log.info("[CACHE MISS] Fetching swimlanes for user: {} (id={})", user.getEmail(), user.getId());
        List<SwimLane> result = swimLaneDAO.findByUserIdAndIsDeletedFalseOrderByPositionAsc(user.getId());
        log.info("[TIMING] getAllSwimLanes() completed in {}ms, returned {} lanes", System.currentTimeMillis() - start,
                result.size());
        return result;
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Cacheable(value = "lanes", key = "'active-' + #root.target.currentUserId")
    public List<SwimLane> getActiveSwimLanes() {
        long start = System.currentTimeMillis();
        User user = getCurrentUser();
        log.info("[CACHE MISS] Fetching ACTIVE swimlanes for user: {} (id={})", user.getEmail(), user.getId());
        List<SwimLane> result = swimLaneDAO
                .findByUserIdAndIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc(user.getId());
        log.info("[TIMING] getActiveSwimLanes() completed in {}ms, returned {} lanes",
                System.currentTimeMillis() - start, result.size());
        return result;
    }

    public List<SwimLane> getCompletedSwimLanes() {
        User user = getCurrentUser();
        return swimLaneDAO.findByUserIdAndIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc(user.getId());
    }

    @Idempotent(keyExpression = "'createLane:' + #swimLane.name")
    @CacheEvict(value = { "lanes", "userLanes" }, allEntries = true)
    public SwimLane createSwimLane(SwimLane swimLane) {
        User user = getCurrentUser();
        log.info("[CACHE EVICT] Invalidating 'lanes' cache - creating new swimlane");
        swimLane.setUser(user);

        // Set position to max + 1 for this user
        Integer maxPos = swimLaneDAO.findMaxPositionByUserId(user.getId());
        swimLane.setPosition(maxPos == null ? 0 : maxPos + 1);
        log.info("Creating new swimlane '{}' for user: {}", swimLane.getName(), user.getEmail());
        return swimLaneDAO.save(swimLane);
    }

    @Idempotent(keyExpression = "'completeLane:' + #id")
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

    @Idempotent(keyExpression = "'deleteLane:' + #id")
    @CacheEvict(value = { "lanes", "userLanes" }, allEntries = true)
    public void deleteSwimLane(Long id) {
        log.info("[CACHE EVICT] Invalidating 'lanes' cache - deleting swimlane {}", id);
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

    @Idempotent(keyExpression = "'reorderLanes:' + #orderedIds.hashCode()")
    @CacheEvict(value = { "lanes", "userLanes" }, allEntries = true)
    public void reorderSwimLanes(List<Long> orderedIds) {
        log.info("[CACHE EVICT] Invalidating 'lanes' cache - reordering lanes");
        User user = getCurrentUser();
        List<SwimLane> lanes = swimLaneDAO.findAllById(orderedIds);

        // Filter to only lanes owned by current user
        lanes = lanes.stream()
                .filter(lane -> lane.getUser() != null && lane.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());

        // Create a map for O(1) lookup
        Map<Long, SwimLane> laneMap = lanes.stream()
                .collect(Collectors.toMap(SwimLane::getId, Function.identity()));

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
