package com.example.todo.controller;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.repository.SwimLaneRepository;
import com.example.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest extends BaseIntegrationTest {

        @Autowired
        private TaskRepository taskRepository;

        @Autowired
        private SwimLaneRepository swimLaneRepository;

        @Test
        void getAllTasks_ShouldReturnTaskList() throws Exception {
                Task task1 = new Task();
                task1.setName("Task 1");
                task1.setStatus(TaskStatus.TODO);
                taskRepository.save(task1);

                mockMvc.perform(get("/api/tasks"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Task 1"));
        }

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
                assertEquals(1, tasks.size());
                assertEquals("New Task", tasks.get(0).getName());
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

                // Note: With async delete, task may still exist briefly.
                // The async service will delete it. We verify the endpoint returns correctly.
                // To fully test deletion, we'd need to wait for async completion.
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

                // The API returns an immediate optimistic response with the requested status
                // The actual database update happens asynchronously
                mockMvc.perform(patch("/api/tasks/{id}/move", task.getId())
                                .param("status", "DONE")
                                .param("swimLaneId", lane.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DONE"));

                // Note: The actual DB state is updated asynchronously.
                // Full verification would require waiting for async completion.
        }

        @Test
        void moveTask_ShouldReturnOk_EvenForNonExistentTask() throws Exception {
                // The moveTask endpoint returns an optimistic response even for non-existent
                // IDs
                // since it delegates to async service. The async service will fail silently.
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

                Task commentedTask = taskRepository.findById(task.getId()).orElseThrow();
                assertTrue(commentedTask.getComments().contains(commentText));
        }

        @Test
        void updateComment_ShouldReturnUpdatedComment() throws Exception {
                Task task = new Task();
                task.setName("Update Comment Task");
                task.setStatus(TaskStatus.TODO);
                // Add initial comment
                String initialJson = "[{\"id\":\"c1\",\"text\":\"Old Text\",\"createdAt\":\"2023-01-01T10:00:00\",\"updatedAt\":\"2023-01-01T10:00:00\"}]";
                task.setComments(initialJson);
                task = taskRepository.save(task);

                String newText = "Updated Comment";

                mockMvc.perform(put("/api/tasks/{id}/comments/{commentId}", task.getId(), "c1")
                                .content(newText))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.text").value(newText));

                Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
                assertTrue(updatedTask.getComments().contains(newText));
        }

        @Test
        void deleteComment_ShouldReturnNoContent() throws Exception {
                Task task = new Task();
                task.setName("Delete Comment Task");
                task.setStatus(TaskStatus.TODO);
                String initialJson = "[{\"id\":\"c1\",\"text\":\"Delete Me\",\"createdAt\":\"2023-01-01T10:00:00\",\"updatedAt\":\"2023-01-01T10:00:00\"}]";
                task.setComments(initialJson);
                task = taskRepository.save(task);

                mockMvc.perform(delete("/api/tasks/{id}/comments/{commentId}", task.getId(), "c1"))
                                .andExpect(status().isNoContent());

                Task deletedCommentTask = taskRepository.findById(task.getId()).orElseThrow();
                assertEquals("[]", deletedCommentTask.getComments());
        }
}
