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
        when(taskDAO.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.updateTask(taskId, updatedInfo);

        assertEquals("New Name", result.getName());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        verify(taskDAO).save(existingTask);
    }

    @Test
    void moveTask_ShouldUpdateStatusAndLane() {
        Long taskId = 1L;
        Long laneId = 2L;
        Task task = new Task();
        task.setId(taskId);
        
        SwimLane lane = new SwimLane();
        lane.setId(laneId);

        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));
        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        taskService.moveTask(taskId, TaskStatus.DONE, laneId);

        assertEquals(TaskStatus.DONE, task.getStatus());
        assertEquals(lane, task.getSwimLane());
    }
}
