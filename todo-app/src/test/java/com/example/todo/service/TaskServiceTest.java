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
    }

    @Test
    void createTask_ShouldPreserveStatus_WhenStatusIsSet() {
        Task task = new Task();
        task.setName("Test Task");
        task.setStatus(TaskStatus.IN_PROGRESS);

        when(taskDAO.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.createTask(task);

        assertEquals(TaskStatus.IN_PROGRESS, createdTask.getStatus());
        verify(taskDAO).save(task);
    }

    @Test
    void updateTask_ShouldThrowException_WhenTaskNotFound() {
        Long taskId = 999L;
        Task updatedInfo = new Task();
        updatedInfo.setName("New Name");

        when(taskDAO.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(taskId, updatedInfo));
    }

    @Test
    void updateTask_ShouldUpdateSwimLane_WhenProvided() {
        Long taskId = 1L;
        Long swimLaneId = 2L;

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setName("Old Name");

        SwimLane swimLane = new SwimLane();
        swimLane.setId(swimLaneId);

        Task updatedInfo = new Task();
        updatedInfo.setName("New Name");
        updatedInfo.setStatus(TaskStatus.DONE);
        updatedInfo.setSwimLane(swimLane);

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(swimLaneDAO.findById(swimLaneId)).thenReturn(Optional.of(swimLane));

        Task result = taskService.updateTask(taskId, updatedInfo);

        assertEquals("New Name", result.getName());
        assertEquals(swimLaneId, result.getSwimLane().getId());
        verify(asyncWriteService).saveTask(existingTask);
    }

    @Test
    void moveTask_ShouldWorkWithoutSwimLaneId() {
        Long taskId = 1L;
        Integer position = 0;

        Task result = taskService.moveTask(taskId, TaskStatus.BLOCKED, null, position);

        assertEquals(TaskStatus.BLOCKED, result.getStatus());
        assertNull(result.getSwimLane());
        assertEquals(position, result.getPosition());
        verify(asyncWriteService).moveTask(taskId, TaskStatus.BLOCKED, null, position);
    }

    @Test
    void addComment_ShouldThrowException_WhenTaskNotFound() {
        Long taskId = 999L;
        when(taskDAO.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.addComment(taskId, "Comment"));
    }

    @Test
    void updateComment_ShouldThrowException_WhenTaskNotFound() {
        Long taskId = 999L;
        when(taskDAO.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.updateComment(taskId, "c1", "Text"));
    }

    @Test
    void deleteComment_ShouldThrowException_WhenTaskNotFound() {
        Long taskId = 999L;
        when(taskDAO.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.deleteComment(taskId, "c1"));
    }

    @Test
    void addComment_ShouldWorkWithEmptyComments() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setComments(""); // Empty string

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        com.example.todo.model.Comment comment = taskService.addComment(taskId, "First Comment");

        assertNotNull(comment);
        assertEquals("First Comment", comment.getText());
        verify(asyncWriteService).saveTask(task);
    }

    @Test
    void addComment_ShouldWorkWithNullComments() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setComments(null); // Null comments

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        com.example.todo.model.Comment comment = taskService.addComment(taskId, "First Comment");

        assertNotNull(comment);
        assertEquals("First Comment", comment.getText());
        verify(asyncWriteService).saveTask(task);
    }

    @Test
    void addComment_ShouldMigrateLegacyStringArrayFormat() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        // Legacy format: array of strings
        task.setComments("[\"Old comment 1\", \"Old comment 2\"]");

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        com.example.todo.model.Comment comment = taskService.addComment(taskId, "New Comment");

        assertNotNull(comment);
        assertEquals("New Comment", comment.getText());
        verify(asyncWriteService).saveTask(task);
        // The task should now have migrated comments + new comment
        assertTrue(task.getComments().contains("Old comment 1"));
        assertTrue(task.getComments().contains("New Comment"));
    }

    @Test
    void updateComment_ShouldThrowException_WhenCommentNotFound() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setComments(
                "[{\"id\":\"c1\",\"text\":\"Text\",\"createdAt\":\"2023-01-01T10:00:00\",\"updatedAt\":\"2023-01-01T10:00:00\"}]");

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        // Try to update a non-existent comment
        assertThrows(RuntimeException.class, () -> taskService.updateComment(taskId, "non-existent-id", "New Text"));
    }

    @Test
    void updateTask_ShouldThrowException_WhenSwimLaneNotFound() {
        Long taskId = 1L;
        Long swimLaneId = 999L;

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setName("Old Name");

        SwimLane swimLane = new SwimLane();
        swimLane.setId(swimLaneId);

        Task updatedInfo = new Task();
        updatedInfo.setName("New Name");
        updatedInfo.setStatus(TaskStatus.DONE);
        updatedInfo.setSwimLane(swimLane);

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(swimLaneDAO.findById(swimLaneId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(taskId, updatedInfo));
    }

    @Test
    void getAllTasks_ShouldReturnAllTasks() {
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(2L);

        when(taskDAO.findAll()).thenReturn(java.util.Arrays.asList(task1, task2));

        java.util.List<Task> result = taskService.getAllTasks();

        assertEquals(2, result.size());
        verify(taskDAO).findAll();
    }

    @Test
    void getTask_ShouldReturnEmpty_WhenNotFound() {
        Long taskId = 999L;

        when(taskDAO.findById(taskId)).thenReturn(Optional.empty());

        Optional<Task> result = taskService.getTask(taskId);

        assertFalse(result.isPresent());
    }

    @Test
    void addComment_ShouldHandleMalformedJson() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setComments("not valid json"); // Malformed JSON

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        // Should not throw, should start with empty list
        com.example.todo.model.Comment comment = taskService.addComment(taskId, "New Comment");

        assertNotNull(comment);
        assertEquals("New Comment", comment.getText());
        verify(asyncWriteService).saveTask(task);
    }
}
