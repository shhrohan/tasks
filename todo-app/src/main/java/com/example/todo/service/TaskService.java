package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.Comment;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.model.User;
import com.example.todo.repository.CommentRepository;
import com.example.todo.repository.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TaskService {

    private final TaskDAO taskDAO;
    private final SwimLaneDAO swimLaneDAO;
    private final CommentRepository commentRepository;
    private final AsyncWriteService asyncWriteService;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    
    // Self-injection for @Cacheable proxy calls within same class
    private final TaskService self;

    public TaskService(TaskDAO taskDAO, SwimLaneDAO swimLaneDAO, CommentRepository commentRepository,
            AsyncWriteService asyncWriteService, UserRepository userRepository, 
            CacheManager cacheManager, @Lazy TaskService self) {
        this.taskDAO = taskDAO;
        this.swimLaneDAO = swimLaneDAO;
        this.commentRepository = commentRepository;
        this.asyncWriteService = asyncWriteService;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.self = self;
    }
    
    // =========================================================================
    // WRITE-THROUGH CACHE HELPER
    // =========================================================================
    
    /**
     * Update a task in the cache without evicting the entire cache.
     * This implements write-through caching for better performance.
     * 
     * @param taskId The ID of the task to update
     * @param updater A function to apply updates to the task
     */
    @SuppressWarnings("unchecked")
    private void updateTaskInCache(Long taskId, java.util.function.Consumer<Task> updater) {
        Cache cache = cacheManager.getCache("tasks");
        if (cache == null) {
            log.warn("[CACHE] 'tasks' cache not found, skipping write-through");
            return;
        }
        
        Cache.ValueWrapper wrapper = cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY);
        if (wrapper == null) {
            log.info("[CACHE] No cached tasks found, skipping write-through update");
            return;
        }
        
        List<Task> cachedTasks = (List<Task>) wrapper.get();
        if (cachedTasks == null) {
            return;
        }
        
        // Find and update the task in the cached list
        for (Task task : cachedTasks) {
            if (task.getId().equals(taskId)) {
                updater.accept(task);
                log.info("[CACHE WRITE-THROUGH] Updated task {} in cache", taskId);
                return;
            }
        }
        log.debug("[CACHE] Task {} not found in cache for write-through update", taskId);
    }
    
    /**
     * Remove a task from the cache without evicting the entire cache.
     */
    @SuppressWarnings("unchecked")
    private void removeTaskFromCache(Long taskId) {
        Cache cache = cacheManager.getCache("tasks");
        if (cache == null) return;
        
        Cache.ValueWrapper wrapper = cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY);
        if (wrapper == null) return;
        
        List<Task> cachedTasks = (List<Task>) wrapper.get();
        if (cachedTasks == null) return;
        
        // Create new list without the deleted task
        List<Task> updatedList = new ArrayList<>(cachedTasks);
        updatedList.removeIf(t -> t.getId().equals(taskId));
        
        // Put the updated list back in cache
        cache.put(org.springframework.cache.interceptor.SimpleKey.EMPTY, updatedList);
        log.info("[CACHE WRITE-THROUGH] Removed task {} from cache", taskId);
    }
    
    /**
     * Add a new task to the cache.
     */
    @SuppressWarnings("unchecked")
    private void addTaskToCache(Task task) {
        Cache cache = cacheManager.getCache("tasks");
        if (cache == null) return;
        
        Cache.ValueWrapper wrapper = cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY);
        if (wrapper == null) return;
        
        List<Task> cachedTasks = (List<Task>) wrapper.get();
        if (cachedTasks == null) return;
        
        // Create new list with the new task
        List<Task> updatedList = new ArrayList<>(cachedTasks);
        updatedList.add(task);
        
        // Put the updated list back in cache
        cache.put(org.springframework.cache.interceptor.SimpleKey.EMPTY, updatedList);
        log.info("[CACHE WRITE-THROUGH] Added task {} to cache", task.getId());
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

    /**
     * Get tasks for the currently authenticated user only.
     * Filters out tasks that belong to swimlanes owned by other users.
     * Uses the cached getAllTasks() to avoid hitting the database on every login.
     */
    public List<Task> getTasksForCurrentUser() {
        long start = System.currentTimeMillis();
        User user = getCurrentUser();
        log.info("[TaskService] Fetching tasks for user: {} (id={})", user.getEmail(), user.getId());
        // Use self-injection to call through Spring proxy for @Cacheable to work
        List<Task> allTasks = self.getAllTasks();
        List<Task> result = allTasks.stream()
                .filter(task -> task.getSwimLane() != null
                        && task.getSwimLane().getUser() != null
                        && task.getSwimLane().getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
        log.info("[TIMING] getTasksForCurrentUser() completed in {}ms, returned {} tasks (filtered from {})",
                System.currentTimeMillis() - start, result.size(), allTasks.size());
        return result;
    }

    @Cacheable(value = "tasks")
    public List<Task> getAllTasks() {
        long start = System.currentTimeMillis();
        log.info("[CACHE MISS] Fetching all tasks from database");
        List<Task> result = taskDAO.findAll();
        log.info("[TIMING] getAllTasks() completed in {}ms, returned {} tasks", System.currentTimeMillis() - start,
                result.size());
        return result;
    }

    @Cacheable(value = "tasksByLane", key = "#swimLaneId")
    @Transactional(readOnly = true)
    public List<Task> getTasksBySwimLaneId(Long swimLaneId) {
        long start = System.currentTimeMillis();
        log.info("[CACHE MISS] Fetching tasks for lane {} from database", swimLaneId);
        List<Task> result = taskDAO.findBySwimLaneId(swimLaneId);
        log.info("[TIMING] getTasksBySwimLaneId({}) completed in {}ms, returned {} tasks", swimLaneId,
                System.currentTimeMillis() - start, result.size());
        return result;
    }

    public Optional<Task> getTask(Long id) {
        return taskDAO.findById(id);
    }

    /**
     * Create a new task and add it to the cache (write-through).
     */
    public Task createTask(Task task) {
        log.info("[CACHE WRITE-THROUGH] Creating new task");
        log.debug("Creating task: {}", task);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        Task savedTask = taskDAO.save(task);
        addTaskToCache(savedTask);
        return savedTask;
    }

    /**
     * Update a task and update the cache (write-through).
     */
    public Task updateTask(Long id, Task updatedTask) {
        log.info("[CACHE WRITE-THROUGH] Updating task {}", id);
        return taskDAO.findById(id)
                .map(existing -> {
                    existing.setName(updatedTask.getName());
                    existing.setStatus(updatedTask.getStatus());
                    existing.setTags(updatedTask.getTags());
                    if (updatedTask.getSwimLane() != null && updatedTask.getSwimLane().getId() != null) {
                        SwimLane lane = swimLaneDAO.findById(updatedTask.getSwimLane().getId())
                                .orElseThrow(() -> {
                                    log.error("SwimLane not found: {}", updatedTask.getSwimLane().getId());
                                    return new IllegalArgumentException("SwimLane not found");
                                });
                        existing.setSwimLane(lane);
                    }
                    
                    // Write-through: Update cache immediately
                    updateTaskInCache(id, cached -> {
                        cached.setName(existing.getName());
                        cached.setStatus(existing.getStatus());
                        cached.setTags(existing.getTags());
                        cached.setSwimLane(existing.getSwimLane());
                    });
                    
                    asyncWriteService.saveTask(existing);
                    log.info("Returning immediate response to UI for task {}", id);
                    return existing;
                })
                .orElseThrow(() -> {
                    log.error("Task not found: {}", id);
                    return new IllegalArgumentException("Task not found: " + id);
                });
    }

    /**
     * Delete a task and remove it from cache (write-through).
     */
    public void deleteTask(Long id) {
        log.info("[CACHE WRITE-THROUGH] Deleting task {}", id);
        removeTaskFromCache(id);
        asyncWriteService.deleteTask(id);
        log.info("Returning immediate response to UI for delete task {}", id);
    }

    /**
     * Move a task and update the cache (write-through).
     */
    @Transactional
    public Task moveTask(Long id, TaskStatus newStatus, Long swimLaneId, Integer position) {
        long start = System.currentTimeMillis();
        log.info("[CACHE WRITE-THROUGH] Moving task {} to status={}, lane={}, position={}", 
                id, newStatus, swimLaneId, position);

        // Write-through: Update cache immediately
        final Long finalSwimLaneId = swimLaneId;
        updateTaskInCache(id, cached -> {
            cached.setStatus(newStatus);
            cached.setPosition(position);
            if (finalSwimLaneId != null) {
                SwimLane lane = cached.getSwimLane();
                if (lane == null) {
                    lane = new SwimLane();
                }
                lane.setId(finalSwimLaneId);
                cached.setSwimLane(lane);
            }
        });

        Task dummyTask = new Task();
        dummyTask.setId(id);
        dummyTask.setStatus(newStatus);
        dummyTask.setPosition(position);

        if (swimLaneId != null) {
            SwimLane lane = new SwimLane();
            lane.setId(swimLaneId);
            dummyTask.setSwimLane(lane);
        }

        asyncWriteService.moveTask(id, newStatus, swimLaneId, position);
        log.info("[TIMING] Optimistic UI response for task {} returned in {}ms", id,
                System.currentTimeMillis() - start);
        return dummyTask;
    }

    // =========================================================================
    // COMMENT CRUD (Using CommentRepository)
    // =========================================================================

    @Transactional
    public Comment addComment(Long taskId, String text) {
        log.info("Adding comment to task {}", taskId);
        Task task = getTask(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        Comment comment = Comment.builder()
                .text(text)
                .task(task)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment {} added to task {}", saved.getId(), taskId);
        return saved;
    }

    @Transactional
    public Comment updateComment(Long taskId, Long commentId, String newText) {
        log.info("Updating comment {} in task {}", commentId, taskId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (!comment.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Comment does not belong to task " + taskId);
        }

        comment.setText(newText);
        Comment updated = commentRepository.save(comment);
        log.info("Comment {} updated", commentId);
        return updated;
    }

    @Transactional
    public void deleteComment(Long taskId, Long commentId) {
        log.info("Deleting comment {} from task {}", commentId, taskId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (!comment.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Comment does not belong to task " + taskId);
        }

        commentRepository.delete(comment);
        log.info("Comment {} deleted", commentId);
    }
}
