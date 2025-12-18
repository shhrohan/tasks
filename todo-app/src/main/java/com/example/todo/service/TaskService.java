package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TaskService {

    private final TaskDAO taskDAO;
    private final SwimLaneDAO swimLaneDAO;
    private final AsyncWriteService asyncWriteService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public TaskService(TaskDAO taskDAO, SwimLaneDAO swimLaneDAO, AsyncWriteService asyncWriteService) {
        this.taskDAO = taskDAO;
        this.swimLaneDAO = swimLaneDAO;
        this.asyncWriteService = asyncWriteService;
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

    @CacheEvict(value = { "tasks", "tasksByLane" }, allEntries = true)
    public Task createTask(Task task) {
        log.info("[CACHE EVICT] Invalidating 'tasks' cache - creating new task");
        log.debug("Creating task: {}", task);
        // Ensure default status if not set
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        return taskDAO.save(task);
    }

    public Task updateTask(Long id, Task updatedTask) {
        log.info("Delegating UPDATE for task {} to Async Service", id);
        return taskDAO.findById(id)
                .map(existing -> {
                    existing.setName(updatedTask.getName());
                    existing.setStatus(updatedTask.getStatus());
                    existing.setComments(updatedTask.getComments());
                    existing.setTags(updatedTask.getTags());
                    if (updatedTask.getSwimLane() != null && updatedTask.getSwimLane().getId() != null) {
                        SwimLane lane = swimLaneDAO.findById(updatedTask.getSwimLane().getId())
                                .orElseThrow(() -> {
                                    log.error("SwimLane not found: {}", updatedTask.getSwimLane().getId());
                                    return new IllegalArgumentException("SwimLane not found");
                                });
                        existing.setSwimLane(lane);
                    }
                    // Fire and forget
                    asyncWriteService.saveTask(existing);
                    log.info("Returning immediate response to UI for task {}", id);
                    return existing;
                })
                .orElseThrow(() -> {
                    log.error("Task not found: {}", id);
                    return new IllegalArgumentException("Task not found: " + id);
                });
    }

    @CacheEvict(value = { "tasks", "tasksByLane" }, allEntries = true)
    public void deleteTask(Long id) {
        log.info("[CACHE EVICT] Invalidating 'tasks' cache - deleting task {}", id);
        log.info("Delegating DELETE for task {} to Async Service", id);
        asyncWriteService.deleteTask(id);
        log.info("Returning immediate response to UI for delete task {}", id);
    }

    @CacheEvict(value = { "tasks", "tasksByLane" }, allEntries = true)
    @Transactional
    public Task moveTask(Long id, TaskStatus newStatus, Long swimLaneId, Integer position) {
        long start = System.currentTimeMillis();
        log.info("[CACHE EVICT] Invalidating 'tasks' cache - moving task {}", id);
        log.info("Delegating MOVE for task {} to Async Service (status={}, lane={}, position={})", id, newStatus,
                swimLaneId, position);

        // Optimization: Skip DB fetch. Construct dummy object for UI response.
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

    public com.example.todo.model.Comment addComment(Long taskId, String text) {
        Task task = getTask(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        try {
            List<com.example.todo.model.Comment> comments = parseComments(task.getComments());
            com.example.todo.model.Comment newComment = com.example.todo.model.Comment.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .text(text)
                    .createdAt(java.time.LocalDateTime.now())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();
            comments.add(newComment);
            task.setComments(objectMapper.writeValueAsString(comments));
            task.setComments(objectMapper.writeValueAsString(comments));
            asyncWriteService.saveTask(task);
            return newComment;
        } catch (Exception e) {
            throw new RuntimeException("Error adding comment", e);
        }
    }

    public com.example.todo.model.Comment updateComment(Long taskId, String commentId, String newText) {
        Task task = getTask(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        try {
            List<com.example.todo.model.Comment> comments = parseComments(task.getComments());
            com.example.todo.model.Comment comment = comments.stream()
                    .filter(c -> c.getId().equals(commentId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

            comment.setText(newText);
            comment.setUpdatedAt(java.time.LocalDateTime.now());

            task.setComments(objectMapper.writeValueAsString(comments));
            task.setComments(objectMapper.writeValueAsString(comments));
            asyncWriteService.saveTask(task);
            return comment;
        } catch (Exception e) {
            throw new RuntimeException("Error updating comment", e);
        }
    }

    public void deleteComment(Long taskId, String commentId) {
        Task task = getTask(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        try {
            List<com.example.todo.model.Comment> comments = parseComments(task.getComments());
            comments.removeIf(c -> c.getId().equals(commentId));
            task.setComments(objectMapper.writeValueAsString(comments));
            task.setComments(objectMapper.writeValueAsString(comments));
            asyncWriteService.saveTask(task);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting comment", e);
        }
    }

    private List<com.example.todo.model.Comment> parseComments(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            // Handle legacy string array if necessary, or assume migration is handled.
            // For robustness, check if it starts with [" (legacy) or [{" (new)
            if (json.trim().startsWith("[\"")) {
                // Legacy: List<String>
                List<String> oldComments = objectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
                List<com.example.todo.model.Comment> newComments = new java.util.ArrayList<>();
                for (String text : oldComments) {
                    newComments.add(com.example.todo.model.Comment.builder()
                            .id(java.util.UUID.randomUUID().toString())
                            .text(text)
                            .createdAt(java.time.LocalDateTime.now())
                            .updatedAt(java.time.LocalDateTime.now())
                            .build());
                }
                return newComments;
            } else {
                return objectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<List<com.example.todo.model.Comment>>() {
                        });
            }
        } catch (Exception e) {
            log.error("Error parsing comments: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
