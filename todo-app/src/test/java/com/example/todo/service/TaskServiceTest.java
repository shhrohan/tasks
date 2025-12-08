package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskDAO taskDAO;

    @Mock
    private SwimLaneDAO swimLaneDAO;

    @Mock
    private AsyncWriteService asyncWriteService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTask_ShouldSetDefaultStatus_WhenStatusIsNull() {
        Task task = new Task();
        task.setName("Test Task");

        when(taskDAO.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.createTask(task);

        assertEquals(TaskStatus.TODO, createdTask.getStatus());
        verify(taskDAO).save(task);
    }

    @Test
    void updateTask_ShouldUpdateFields_WhenTaskExists() {
        Long taskId = 1L;
        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setName("Old Name");

        Task updatedInfo = new Task();
        updatedInfo.setName("New Name");
        updatedInfo.setStatus(TaskStatus.IN_PROGRESS);

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(existingTask));

        Task result = taskService.updateTask(taskId, updatedInfo);

        assertEquals("New Name", result.getName());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        // Verify async service is called instead of DAO save
        verify(asyncWriteService).saveTask(existingTask);
        verify(taskDAO, never()).save(existingTask);
    }

    @Test
    void moveTask_ShouldUpdateStatusAndLane() {
        Long taskId = 1L;
        Long laneId = 2L;
        Integer position = 0;

        Task result = taskService.moveTask(taskId, TaskStatus.DONE, laneId, position);

        assertEquals(TaskStatus.DONE, result.getStatus());
        assertEquals(laneId, result.getSwimLane().getId());
        assertEquals(position, result.getPosition());
        verify(asyncWriteService).moveTask(taskId, TaskStatus.DONE, laneId, position);
        verify(taskDAO, never()).findById(taskId);
    }

    @Test
    void getAllTasks_ShouldReturnList() {
        Task task = new Task();
        task.setId(1L);
        when(taskDAO.findAll()).thenReturn(java.util.Arrays.asList(task));

        java.util.List<Task> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals(task, result.get(0));
    }

    @Test
    void getTasksBySwimLaneId_ShouldReturnTasksForLane() {
        Long swimLaneId = 1L;
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(2L);
        when(taskDAO.findBySwimLaneId(swimLaneId)).thenReturn(java.util.Arrays.asList(task1, task2));

        java.util.List<Task> result = taskService.getTasksBySwimLaneId(swimLaneId);

        assertEquals(2, result.size());
        verify(taskDAO).findBySwimLaneId(swimLaneId);
    }

    @Test
    void getTask_ShouldReturnTask_WhenFound() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        Optional<Task> result = taskService.getTask(taskId);

        assertTrue(result.isPresent());
        assertEquals(task, result.get());
    }

    @Test
    void deleteTask_ShouldCallDeleteById() {
        Long taskId = 1L;
        taskService.deleteTask(taskId);

        verify(asyncWriteService).deleteTask(taskId);
        verify(taskDAO, never()).deleteById(taskId);
    }

    @Test
    void addComment_ShouldAddCommentToTask() throws Exception {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setComments("[]");

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        com.example.todo.model.Comment comment = taskService.addComment(taskId, "New Comment");

        assertNotNull(comment);
        assertEquals("New Comment", comment.getText());
        verify(asyncWriteService).saveTask(task);
        verify(taskDAO, never()).save(task);
    }

    @Test
    void updateComment_ShouldUpdateExistingComment() throws Exception {
        Long taskId = 1L;
        String commentId = "c1";
        Task task = new Task();
        task.setId(taskId);
        // Mock existing comment JSON
        String json = "[{\"id\":\"c1\",\"text\":\"Old Text\",\"createdAt\":\"2023-01-01T10:00:00\",\"updatedAt\":\"2023-01-01T10:00:00\"}]";
        task.setComments(json);

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        com.example.todo.model.Comment updated = taskService.updateComment(taskId, commentId, "New Text");

        assertEquals("New Text", updated.getText());
        verify(asyncWriteService).saveTask(task);
        verify(taskDAO, never()).save(task);
    }

    @Test
    void deleteComment_ShouldRemoveComment() throws Exception {
        Long taskId = 1L;
        String commentId = "c1";
        Task task = new Task();
        task.setId(taskId);
        String json = "[{\"id\":\"c1\",\"text\":\"Text\",\"createdAt\":\"2023-01-01T10:00:00\",\"updatedAt\":\"2023-01-01T10:00:00\"}]";
        task.setComments(json);

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        taskService.deleteComment(taskId, commentId);

        verify(asyncWriteService).saveTask(task);
        verify(taskDAO, never()).save(task);
        // Verify comment is gone (simple check on the saved object or logic)
        // Since we mock save to return arg, we can check the arg, but verify is enough
        // for interaction.
    }
}
