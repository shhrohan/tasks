package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * CacheWarmupService - Pre-warms caches on application startup.
 * 
 * This ensures that the first request for each user gets a cache hit
 * instead of a slow database query.
 * 
 * Caches warmed (in parallel for speed):
 * - lanesByUser: Swimlanes per user
 * - tasksByLane: Tasks per lane
 */
@Service
@Log4j2
public class CacheWarmupService {

    private final UserDAO userDAO;
    private final SwimLaneService swimLaneService;
    private final TaskService taskService;

    public CacheWarmupService(UserDAO userDAO, SwimLaneService swimLaneService, TaskService taskService) {
        this.userDAO = userDAO;
        this.swimLaneService = swimLaneService;
        this.taskService = taskService;
    }

    /**
     * Pre-warm caches after application is fully started.
     * Uses parallel streams for concurrent DB calls to speed up startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("[CACHE WARMUP] ====== Starting cache pre-warming ======");

        try {
            // Get all users
            List<User> users = userDAO.findAll();
            log.info("[CACHE WARMUP] Found {} users to pre-warm caches", users.size());

            // Pre-warm lanesByUser cache (parallel calls)
            long start = System.currentTimeMillis();
            List<SwimLane> allLanes = users.parallelStream()
                    .flatMap(user -> swimLaneService.getActiveSwimLanesForUser(user.getId()).stream())
                    .collect(Collectors.toList());
            log.info("[CACHE WARMUP] Cached {} total lanes for {} users in {}ms (parallel)", 
                    allLanes.size(), users.size(), System.currentTimeMillis() - start);

            // Pre-warm tasksByLane cache (parallel calls)
            log.info("[CACHE WARMUP] Pre-warming tasksByLane cache for {} lanes (parallel)...", allLanes.size());
            start = System.currentTimeMillis();
            AtomicInteger totalTasks = new AtomicInteger(0);
            allLanes.parallelStream().forEach(lane -> {
                int taskCount = taskService.getTasksBySwimLaneId(lane.getId()).size();
                totalTasks.addAndGet(taskCount);
            });
            log.info("[CACHE WARMUP] Cached {} total tasks across {} lanes in {}ms (parallel)", 
                    totalTasks.get(), allLanes.size(), System.currentTimeMillis() - start);

            log.info("[CACHE WARMUP] ====== Cache pre-warming complete ======");
        } catch (Exception e) {
            log.warn("[CACHE WARMUP] Error during cache warmup (non-fatal): {}", e.getMessage());
        }
    }
}
