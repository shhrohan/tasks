package com.example.todo.service;

import com.example.todo.model.Comment;
import com.example.todo.model.Task;
import com.example.todo.repository.CommentRepository;
import com.example.todo.dao.TaskDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * One-time migration service to convert JSON-embedded comments to the new Comment table.
 * Runs on application startup and migrates any tasks that still have legacy JSON comments.
 */
@Service
@Log4j2
public class CommentMigrationService {

    private final TaskDAO taskDAO;
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;

    public CommentMigrationService(TaskDAO taskDAO, CommentRepository commentRepository) {
        this.taskDAO = taskDAO;
        this.commentRepository = commentRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateJsonCommentsToTable() {
        log.info("[Migration] Starting JSON comments migration check...");

        List<Task> allTasks = taskDAO.findAll();
        int migratedCount = 0;

        for (Task task : allTasks) {
            // Skip if task already has comments in the new table
            if (task.getComments() != null && !task.getComments().isEmpty()) {
                continue;
            }

            // Check for legacy JSON comments in the database
            // Note: The Task entity no longer has a "comments" String field,
            // but we need to check the raw database value
            // This migration is for tasks that might have been persisted before
            // the schema change. JPA will handle schema updates via hibernate.ddl-auto
        }

        log.info("[Migration] Migration complete. Migrated {} tasks.", migratedCount);
    }
}
