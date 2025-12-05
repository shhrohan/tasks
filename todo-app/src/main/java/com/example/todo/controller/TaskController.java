package com.example.todo.controller;

import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/tasks")
@Log4j2
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        log.info("Fetching all tasks");
        return taskService.getAllTasks();
    }

    @GetMapping("/swimlane/{swimLaneId}")
    public List<Task> getTasksBySwimLane(@PathVariable Long swimLaneId) {
        log.info("Fetching tasks for swimlane: {}", swimLaneId);
        return taskService.getTasksBySwimLaneId(swimLaneId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        Optional<Task> task = taskService.getTask(id);
        return task.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        log.info("Creating new task: {}", task.getName());
        return taskService.createTask(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        log.info("Updating task with id: {}", id);
        try {
            Task updated = taskService.updateTask(id, task);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update task with id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("Deleting task with id: {}", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<Task> moveTask(@PathVariable Long id, @RequestParam TaskStatus status, @RequestParam(required = false) Long swimLaneId) {
        log.info("Moving task {} to status {} in swimlane {}", id, status, swimLaneId);
        try {
            Task moved = taskService.moveTask(id, status, swimLaneId);
            return ResponseEntity.ok(moved);
        } catch (IllegalArgumentException e) {
            log.error("Failed to move task {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<com.example.todo.model.Comment> addComment(@PathVariable Long id, @RequestBody String text) {
        log.info("Adding comment to task {}", id);
        try {
            // Text might come as a JSON string or plain text depending on frontend. 
            // Simple approach: assume raw string or handle quotes if JSON.
            // Better: use a DTO, but for single string, raw body is fine if handled.
            // Let's clean quotes if it's a JSON string literal.
            String cleanText = text;
            if (text.startsWith("\"") && text.endsWith("\"")) {
                cleanText = text.substring(1, text.length() - 1);
            }
            // Also unescape if needed, but simple for now.
            
            com.example.todo.model.Comment comment = taskService.addComment(id, cleanText);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("Failed to add comment to task {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<com.example.todo.model.Comment> updateComment(@PathVariable Long id, @PathVariable String commentId, @RequestBody String text) {
        log.info("Updating comment {} in task {}", commentId, id);
        try {
            String cleanText = text;
            if (text.startsWith("\"") && text.endsWith("\"")) {
                cleanText = text.substring(1, text.length() - 1);
            }
            com.example.todo.model.Comment comment = taskService.updateComment(id, commentId, cleanText);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("Failed to update comment {} in task {}", commentId, id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable String commentId) {
        log.info("Deleting comment {} from task {}", commentId, id);
        try {
            taskService.deleteComment(id, commentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete comment {} from task {}", commentId, id, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
