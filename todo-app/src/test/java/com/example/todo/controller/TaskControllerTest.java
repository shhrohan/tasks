package com.example.todo.controller;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.Comment;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.repository.CommentRepository;
import com.example.todo.repository.SwimLaneRepository;
import com.example.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
class TaskControllerTest extends BaseIntegrationTest {

        @Autowired
        private TaskRepository taskRepository;

        @Autowired
        private SwimLaneRepository swimLaneRepository;

        @Autowired
        private CommentRepository commentRepository;

        @Test
        void createTask_ShouldReturnCreatedTask() throws Exception {
                Task task = new Task();
                task.setName("New Task");

                mockMvc.perform(post("/api/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(task)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("New Task"));

                List<Task> tasks = taskRepository.findAll();
                assertTrue(tasks.size() >= 1, "At least one task should exist");
                assertTrue(tasks.stream().anyMatch(t -> "New Task".equals(t.getName())),
                                "Created task should exist in repository");
        }

        @Test
        void updateTask_ShouldReturnUpdatedTask() throws Exception {
                Task task = new Task();
                task.setName("Old Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                Task updatedInfo = new Task();
                updatedInfo.setName("Updated Task");
                updatedInfo.setStatus(TaskStatus.IN_PROGRESS);

                mockMvc.perform(put("/api/tasks/{id}", task.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedInfo)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Task"));

                Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
                assertEquals("Updated Task", updatedTask.getName());
        }

        @Test
        void getTask_ShouldReturnTask_WhenFound() throws Exception {
                Task task = new Task();
                task.setName("Existing Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                mockMvc.perform(get("/api/tasks/{id}", task.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Existing Task"));
        }

        @Test
        void getTask_ShouldReturnNotFound_WhenNotFound() throws Exception {
                mockMvc.perform(get("/api/tasks/{id}", 999L))
                                .andExpect(status().isNotFound());
        }

        @Test
        void deleteTask_ShouldReturnNoContent() throws Exception {
                Task task = new Task();
                task.setName("Delete Me");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                mockMvc.perform(delete("/api/tasks/{id}", task.getId()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void moveTask_ShouldReturnMovedTask() throws Exception {
                SwimLane lane = new SwimLane();
                lane.setName("Lane 1");
                lane = swimLaneRepository.save(lane);

                Task task = new Task();
                task.setName("Move Me");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                mockMvc.perform(patch("/api/tasks/{id}/move", task.getId())
                                .param("status", "DONE")
                                .param("swimLaneId", lane.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        void moveTask_ShouldReturnOk_EvenForNonExistentTask() throws Exception {
                mockMvc.perform(patch("/api/tasks/{id}/move", 999L)
                                .param("status", "DONE"))
                                .andExpect(status().isOk());
        }

        @Test
        void addComment_ShouldReturnComment() throws Exception {
                Task task = new Task();
                task.setName("Comment Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                String commentText = "New Comment";

                mockMvc.perform(post("/api/tasks/{id}/comments", task.getId())
                                .content(commentText))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.text").value(commentText));

                // Verify comment was saved in repository
                List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
                assertEquals(1, comments.size());
                assertEquals(commentText, comments.get(0).getText());
        }

        @Test
        void updateComment_ShouldReturnUpdatedComment() throws Exception {
                Task task = new Task();
                task.setName("Update Comment Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                // Create a comment via repository
                Comment comment = Comment.builder()
                                .text("Old Text")
                                .task(task)
                                .build();
                comment = commentRepository.save(comment);

                String newText = "Updated Comment";

                mockMvc.perform(put("/api/tasks/{id}/comments/{commentId}", task.getId(), comment.getId())
                                .content(newText))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.text").value(newText));

                // Verify update
                Comment updatedComment = commentRepository.findById(comment.getId()).orElseThrow();
                assertEquals(newText, updatedComment.getText());
        }

        @Test
        void deleteComment_ShouldReturnNoContent() throws Exception {
                Task task = new Task();
                task.setName("Delete Comment Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                // Create a comment
                Comment comment = Comment.builder()
                                .text("Delete Me")
                                .task(task)
                                .build();
                comment = commentRepository.save(comment);

                mockMvc.perform(delete("/api/tasks/{id}/comments/{commentId}", task.getId(), comment.getId()))
                                .andExpect(status().isNoContent());

                // Verify deletion
                assertFalse(commentRepository.findById(comment.getId()).isPresent());
        }

        @Test
        void getTasksBySwimLane_ShouldReturnTasksForLane() throws Exception {
                SwimLane lane = new SwimLane();
                lane.setName("Test Lane");
                lane = swimLaneRepository.save(lane);

                Task task = new Task();
                task.setName("Task in Lane");
                task.setStatus(TaskStatus.TODO);
                task.setSwimLane(lane);
                taskRepository.save(task);

                mockMvc.perform(get("/api/tasks/swimlane/{id}", lane.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[?(@.name == 'Task in Lane')]").exists());
        }

        @Test
        void moveTask_ShouldWorkWithPositionParameter() throws Exception {
                SwimLane lane = new SwimLane();
                lane.setName("Lane for Position");
                lane = swimLaneRepository.save(lane);

                Task task = new Task();
                task.setName("Task with Position");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                mockMvc.perform(patch("/api/tasks/{id}/move", task.getId())
                                .param("status", "IN_PROGRESS")
                                .param("swimLaneId", lane.getId().toString())
                                .param("position", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                                .andExpect(jsonPath("$.position").value(5));
        }

        @Test
        void updateTask_ShouldReturnNotFound_WhenTaskDoesNotExist() throws Exception {
                Task updatedInfo = new Task();
                updatedInfo.setName("Some Name");
                updatedInfo.setStatus(TaskStatus.TODO);

                mockMvc.perform(put("/api/tasks/{id}", 9999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedInfo)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void createTask_ShouldSetDefaultStatus_WhenNotProvided() throws Exception {
                Task task = new Task();
                task.setName("Task without Status");

                mockMvc.perform(post("/api/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(task)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("TODO"));
        }

        @Test
        void addComment_ShouldHandleJsonWrappedText() throws Exception {
                Task task = new Task();
                task.setName("Comment Task JSON");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                // Text wrapped in JSON quotes
                mockMvc.perform(post("/api/tasks/{id}/comments", task.getId())
                                .content("\"New Comment\""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.text").value("New Comment"));
        }

        @Test
        void updateComment_ShouldReturnBadRequest_WhenCommentNotFound() throws Exception {
                Task task = new Task();
                task.setName("Update Comment Error Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                mockMvc.perform(put("/api/tasks/{id}/comments/{commentId}", task.getId(), 9999L)
                                .content("Updated Text"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void deleteComment_ShouldReturnBadRequest_WhenTaskNotFound() throws Exception {
                mockMvc.perform(delete("/api/tasks/{id}/comments/{commentId}", 9999L, 1L))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void addComment_ShouldReturnBadRequest_WhenTaskNotFound() throws Exception {
                mockMvc.perform(post("/api/tasks/{id}/comments", 9999L)
                                .content("Test Comment"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updateComment_ShouldHandleJsonWrappedText() throws Exception {
                Task task = new Task();
                task.setName("Update JSON Comment Task");
                task.setStatus(TaskStatus.TODO);
                task = taskRepository.save(task);

                // Create a comment
                Comment comment = Comment.builder()
                                .text("Old")
                                .task(task)
                                .build();
                comment = commentRepository.save(comment);

                // Text wrapped in JSON quotes
                mockMvc.perform(put("/api/tasks/{id}/comments/{commentId}", task.getId(), comment.getId())
                                .content("\"New Text\""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.text").value("New Text"));
        }
}
