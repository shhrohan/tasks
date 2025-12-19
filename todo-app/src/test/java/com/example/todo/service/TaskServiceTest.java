package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.Comment;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.model.User;
import com.example.todo.repository.CommentRepository;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private CommentRepository commentRepository;

    @Mock
    private AsyncWriteService asyncWriteService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TaskService taskService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("testuser@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser@example.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(testUser));
    }

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
    void deleteTask_ShouldCallAsyncService() {
        Long taskId = 1L;
        taskService.deleteTask(taskId);

        verify(asyncWriteService).deleteTask(taskId);
        verify(taskDAO, never()).deleteById(taskId);
    }

    @Test
    void addComment_ShouldSaveCommentViaRepository() {
        Long taskId = 1L;
        Task task = new Task();
        task.setId(taskId);
        task.setComments(new ArrayList<>());

        Comment savedComment = Comment.builder()
                .id(1L)
                .text("New Comment")
                .task(task)
                .build();

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = taskService.addComment(taskId, "New Comment");

        assertNotNull(result);
        assertEquals("New Comment", result.getText());
        assertEquals(1L, result.getId());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void updateComment_ShouldUpdateViaRepository() {
        Long taskId = 1L;
        Long commentId = 1L;

        Task task = new Task();
        task.setId(taskId);

        Comment existingComment = Comment.builder()
                .id(commentId)
                .text("Old Text")
                .task(task)
                .build();

        Comment updatedComment = Comment.builder()
                .id(commentId)
                .text("New Text")
                .task(task)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(existingComment)).thenReturn(updatedComment);

        Comment result = taskService.updateComment(taskId, commentId, "New Text");

        assertEquals("New Text", result.getText());
        verify(commentRepository).save(existingComment);
    }

    @Test
    void deleteComment_ShouldDeleteViaRepository() {
        Long taskId = 1L;
        Long commentId = 1L;

        Task task = new Task();
        task.setId(taskId);

        Comment comment = Comment.builder()
                .id(commentId)
                .text("To be deleted")
                .task(task)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        taskService.deleteComment(taskId, commentId);

        verify(commentRepository).delete(comment);
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
    void updateComment_ShouldThrowException_WhenCommentNotFound() {
        Long taskId = 1L;
        Long commentId = 999L;
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.updateComment(taskId, commentId, "Text"));
    }

    @Test
    void deleteComment_ShouldThrowException_WhenCommentNotFound() {
        Long taskId = 1L;
        Long commentId = 999L;
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> taskService.deleteComment(taskId, commentId));
    }

    @Test
    void updateComment_ShouldThrowException_WhenCommentBelongsToDifferentTask() {
        Long taskId = 1L;
        Long commentId = 1L;
        Long otherTaskId = 2L;

        Task otherTask = new Task();
        otherTask.setId(otherTaskId);

        Comment comment = Comment.builder()
                .id(commentId)
                .text("Text")
                .task(otherTask)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(IllegalArgumentException.class, () -> taskService.updateComment(taskId, commentId, "New Text"));
    }

    @Test
    void deleteComment_ShouldThrowException_WhenCommentBelongsToDifferentTask() {
        Long taskId = 1L;
        Long commentId = 1L;
        Long otherTaskId = 2L;

        Task otherTask = new Task();
        otherTask.setId(otherTaskId);

        Comment comment = Comment.builder()
                .id(commentId)
                .text("Text")
                .task(otherTask)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(IllegalArgumentException.class, () -> taskService.deleteComment(taskId, commentId));
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
    void getTasksForCurrentUser_ShouldReturnOnlyCurrentUserTasks() {
        setupSecurityContext();

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        SwimLane myLane = new SwimLane();
        myLane.setId(1L);
        myLane.setUser(testUser);

        SwimLane otherLane = new SwimLane();
        otherLane.setId(2L);
        otherLane.setUser(otherUser);

        Task myTask = new Task();
        myTask.setId(1L);
        myTask.setName("My Task");
        myTask.setSwimLane(myLane);

        Task otherUserTask = new Task();
        otherUserTask.setId(2L);
        otherUserTask.setName("Other User Task");
        otherUserTask.setSwimLane(otherLane);

        when(taskDAO.findAll()).thenReturn(Arrays.asList(myTask, otherUserTask));

        List<Task> result = taskService.getTasksForCurrentUser();

        assertEquals(1, result.size());
        assertEquals("My Task", result.get(0).getName());
        verify(taskDAO).findAll();
    }

    @Test
    void getTasksForCurrentUser_ShouldFilterOutTasksWithNullSwimLane() {
        setupSecurityContext();

        SwimLane myLane = new SwimLane();
        myLane.setId(1L);
        myLane.setUser(testUser);

        Task myTask = new Task();
        myTask.setId(1L);
        myTask.setSwimLane(myLane);

        Task taskWithoutLane = new Task();
        taskWithoutLane.setId(2L);
        taskWithoutLane.setSwimLane(null);

        when(taskDAO.findAll()).thenReturn(Arrays.asList(myTask, taskWithoutLane));

        List<Task> result = taskService.getTasksForCurrentUser();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getTasksForCurrentUser_ShouldFilterOutTasksWithNullUser() {
        setupSecurityContext();

        SwimLane myLane = new SwimLane();
        myLane.setId(1L);
        myLane.setUser(testUser);

        SwimLane laneWithoutUser = new SwimLane();
        laneWithoutUser.setId(2L);
        laneWithoutUser.setUser(null);

        Task myTask = new Task();
        myTask.setId(1L);
        myTask.setSwimLane(myLane);

        Task taskWithNullUser = new Task();
        taskWithNullUser.setId(2L);
        taskWithNullUser.setSwimLane(laneWithoutUser);

        when(taskDAO.findAll()).thenReturn(Arrays.asList(myTask, taskWithNullUser));

        List<Task> result = taskService.getTasksForCurrentUser();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getTasksForCurrentUser_ShouldReturnEmptyListWhenNoMatchingTasks() {
        setupSecurityContext();

        User otherUser = new User();
        otherUser.setId(2L);

        SwimLane otherLane = new SwimLane();
        otherLane.setId(1L);
        otherLane.setUser(otherUser);

        Task otherTask = new Task();
        otherTask.setId(1L);
        otherTask.setSwimLane(otherLane);

        when(taskDAO.findAll()).thenReturn(Arrays.asList(otherTask));

        List<Task> result = taskService.getTasksForCurrentUser();

        assertTrue(result.isEmpty());
    }
}
