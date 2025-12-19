package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.Comment;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.repository.CommentRepository;
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
    private final CommentRepository commentRepository;
    private final AsyncWriteService asyncWriteService;

    public TaskService(TaskDAO taskDAO, SwimLaneDAO swimLaneDAO, CommentRepository commentRepository,
            AsyncWriteService asyncWriteService) {
        this.taskDAO = taskDAO;
        this.swimLaneDAO = swimLaneDAO;
        this.commentRepository = commentRepository;
        this.asyncWriteService = asyncWriteService;
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
                    existing.setTags(updatedTask.getTags());
                    if (updatedTask.getSwimLane() != null && updatedTask.getSwimLane().getId() != null) {
                        SwimLane lane = swimLaneDAO.findById(updatedTask.getSwimLane().getId())
                                .orElseThrow(() -> {
                                    log.error("SwimLane not found: {}", updatedTask.getSwimLane().getId());
                                    return new IllegalArgumentException("SwimLane not found");
                                });
                        existing.setSwimLane(lane);
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
