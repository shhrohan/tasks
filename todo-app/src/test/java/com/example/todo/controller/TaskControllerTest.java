package com.example.todo.controller;

import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllTasks_ShouldReturnTaskList() throws Exception {
        Task task1 = new Task();
        task1.setId(1L);
        task1.setName("Task 1");

        when(taskService.getAllTasks()).thenReturn(Arrays.asList(task1));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Task 1"));
    }

    @Test
    void createTask_ShouldReturnCreatedTask() throws Exception {
        Task task = new Task();
        task.setName("New Task");

        when(taskService.createTask(any(Task.class))).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Task"));
    }

    @Test
    void updateTask_ShouldReturnUpdatedTask() throws Exception {
        Long taskId = 1L;
        Task task = new Task();
        task.setName("Updated Task");

        when(taskService.updateTask(eq(taskId), any(Task.class))).thenReturn(task);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Task"));
    }

    @Test
    void getTask_ShouldReturnTask_WhenFound() throws Exception {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setName("Existing Task");

        when(taskService.getTask(taskId)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Existing Task"));
    }

    @Test
    void getTask_ShouldReturnNotFound_WhenNotFound() throws Exception {
        Long taskId = 99L;
        when(taskService.getTask(taskId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_ShouldReturnNoContent() throws Exception {
        Long taskId = 1L;
        doNothing().when(taskService).deleteTask(taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(taskId);
    }

    @Test
    void moveTask_ShouldReturnMovedTask() throws Exception {
        Long taskId = 1L;
        Long laneId = 2L;
        Task task = new Task();
        task.setId(taskId);
        task.setStatus(TaskStatus.DONE);

        when(taskService.moveTask(taskId, TaskStatus.DONE, laneId)).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/{id}/move", taskId)
                .param("status", "DONE")
                .param("swimLaneId", laneId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void moveTask_ShouldReturnNotFound_WhenExceptionOccurs() throws Exception {
        Long taskId = 1L;
        when(taskService.moveTask(any(), any(), any())).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(patch("/api/tasks/{id}/move", taskId)
                .param("status", "DONE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addComment_ShouldReturnComment() throws Exception {
        Long taskId = 1L;
        String commentText = "New Comment";
        com.example.todo.model.Comment comment = com.example.todo.model.Comment.builder()
                .id("c1")
                .text(commentText)
                .build();

        when(taskService.addComment(eq(taskId), anyString())).thenReturn(comment);

        mockMvc.perform(post("/api/tasks/{id}/comments", taskId)
                .content(commentText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(commentText));
    }

    @Test
    void updateComment_ShouldReturnUpdatedComment() throws Exception {
        Long taskId = 1L;
        String commentId = "c1";
        String newText = "Updated Comment";
        com.example.todo.model.Comment comment = com.example.todo.model.Comment.builder()
                .id(commentId)
                .text(newText)
                .build();

        when(taskService.updateComment(eq(taskId), eq(commentId), anyString())).thenReturn(comment);

        mockMvc.perform(put("/api/tasks/{id}/comments/{commentId}", taskId, commentId)
                .content(newText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(newText));
    }

    @Test
    void deleteComment_ShouldReturnNoContent() throws Exception {
        Long taskId = 1L;
        String commentId = "c1";
        doNothing().when(taskService).deleteComment(taskId, commentId);

        mockMvc.perform(delete("/api/tasks/{id}/comments/{commentId}", taskId, commentId))
                .andExpect(status().isNoContent());
    }
}
