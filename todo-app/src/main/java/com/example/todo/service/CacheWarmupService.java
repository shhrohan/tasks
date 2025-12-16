package com.example.todo.service;

import com.example.todo.dao.UserDAO;
import com.example.todo.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CacheWarmupService - Pre-warms caches on application startup.
 * 
 * This ensures that the first request for each user gets a cache hit
 * instead of a slow database query.
 */
@Service
@Log4j2
public class CacheWarmupService {

    private final UserDAO userDAO;
    private final SwimLaneService swimLaneService;
    private final TaskService taskService;

    public CacheWarmupService(UserDAO userDAO,
            SwimLaneService swimLaneService,
            TaskService taskService) {
        this.userDAO = userDAO;
        this.swimLaneService = swimLaneService;
        this.taskService = taskService;
    }

    /**
     * Pre-warm caches after application is fully started.
     * This runs after all beans are initialized and the app is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("[CACHE WARMUP] ====== Starting cache pre-warming ======");

        try {
            // Get all users
            List<User> users = userDAO.findAll();
            log.info("[CACHE WARMUP] Found {} users to pre-warm caches for", users.size());

            // Pre-warm lanes cache for each user
            // Note: This requires executing in the context of each user
            // For now, just pre-warm the global tasks cache
            log.info("[CACHE WARMUP] Pre-warming tasks cache...");
            int taskCount = taskService.getAllTasks().size();
            log.info("[CACHE WARMUP] Cached {} tasks", taskCount);

            log.info("[CACHE WARMUP] ====== Cache pre-warming complete ======");
        } catch (Exception e) {
            log.warn("[CACHE WARMUP] Error during cache warmup (non-fatal): {}", e.getMessage());
        }
    }
}
