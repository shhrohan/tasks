package com.example.todo.service;

import com.example.todo.annotation.Idempotent;
import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.Comment;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.repository.CommentRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import lombok.extern.log4j.Log4j2;

/**
 * TaskService - Manages task CRUD operations.
 * 
 * Cache Strategy:
 * - Uses only 'tasksByLane' cache (keyed by swimLaneId)
 * - Cache is evicted on create/update/delete operations
 * - No global 'tasks' cache - tasks are always fetched per lane
 */
@Service
@Log4j2
public class TaskService {

    private final TaskDAO taskDAO;
    private final SwimLaneDAO swimLaneDAO;
    private final CommentRepository commentRepository;
    private final AsyncWriteService asyncWriteService;
    private final CacheManager cacheManager;

    public TaskService(TaskDAO taskDAO, SwimLaneDAO swimLaneDAO, CommentRepository commentRepository,
            AsyncWriteService asyncWriteService, CacheManager cacheManager) {
        this.taskDAO = taskDAO;
        this.swimLaneDAO = swimLaneDAO;
        this.commentRepository = commentRepository;
        this.asyncWriteService = asyncWriteService;
        this.cacheManager = cacheManager;
    }

    // =========================================================================
    // CACHE EVICTION HELPER
    // =========================================================================

    /**
     * Evict a specific lane from the tasksByLane cache.
     */
    private void evictTasksByLaneCache(Long laneId) {
        if (laneId == null) return;
        var cache = cacheManager.getCache("tasksByLane");
        if (cache != null) {
            cache.evict(laneId);
            log.info("[CACHE EVICT] Evicted tasksByLane cache for lane {}", laneId);
        }
    }

    // =========================================================================
    // READ OPERATIONS
    // =========================================================================

    /**
     * Get tasks for a specific swimlane (cached).
     */
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

    // =========================================================================
    // WRITE OPERATIONS (with cache eviction)
    // =========================================================================

    /**
     * Create a new task and evict the lane cache.
     * Protected by @Idempotent to prevent duplicate tasks from rapid clicks.
     */
    @Idempotent(keyExpression = "'createTask:' + #task.name + ':' + (#task.swimLane != null ? #task.swimLane.id : 'null')")
    public Task createTask(Task task) {
        log.info("Creating new task");
        log.debug("Creating task: {}", task);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        Task savedTask = taskDAO.save(task);
        
        // Evict the lane cache so next fetch gets updated data
        if (savedTask.getSwimLane() != null) {
            evictTasksByLaneCache(savedTask.getSwimLane().getId());
        }
        
        return savedTask;
    }

    /**
     * Update a task and evict the lane cache.
     */
    public Task updateTask(Long id, Task updatedTask) {
        log.info("Updating task {}", id);
        return taskDAO.findById(id)
                .map(existing -> {
                    Long oldLaneId = existing.getSwimLane() != null ? existing.getSwimLane().getId() : null;
                    
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

                    // Evict cache for both old and new lane
                    evictTasksByLaneCache(oldLaneId);
                    if (existing.getSwimLane() != null) {
                        evictTasksByLaneCache(existing.getSwimLane().getId());
                    }

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
     * Delete a task and evict the lane cache.
     */
    @Idempotent(keyExpression = "'deleteTask:' + #id")
    public void deleteTask(Long id) {
        log.info("Deleting task {}", id);
        
        // Get the task to find its lane before deletion
        taskDAO.findById(id).ifPresent(task -> {
            if (task.getSwimLane() != null) {
                evictTasksByLaneCache(task.getSwimLane().getId());
            }
        });
        
        asyncWriteService.deleteTask(id);
        log.info("Returning immediate response to UI for delete task {}", id);
    }

    /**
     * Move a task between statuses/lanes.
     */
    @Transactional
    public Task moveTask(Long id, TaskStatus newStatus, Long swimLaneId, Integer position) {
        long start = System.currentTimeMillis();
        log.info("Moving task {} to status={}, lane={}, position={}", id, newStatus, swimLaneId, position);

        // Get old lane ID for cache eviction
        Task existingTask = taskDAO.findById(id).orElse(null);
        Long oldLaneId = (existingTask != null && existingTask.getSwimLane() != null) 
                ? existingTask.getSwimLane().getId() : null;

        // Evict cache for both old and new lane
        evictTasksByLaneCache(oldLaneId);
        if (swimLaneId != null && !swimLaneId.equals(oldLaneId)) {
            evictTasksByLaneCache(swimLaneId);
        }

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

    @Idempotent(keyExpression = "'addComment:' + #taskId + ':' + #text.hashCode()")
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

    @Idempotent(keyExpression = "'updateComment:' + #commentId + ':' + #newText.hashCode()")
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

    @Idempotent(keyExpression = "'deleteComment:' + #taskId + ':' + #commentId")
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
