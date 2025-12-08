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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncWriteServiceTest {

    @Mock
    private TaskDAO taskDAO;

    @Mock
    private SwimLaneDAO swimLaneDAO;

    @Mock
    private SseService sseService;

    @InjectMocks
    private AsyncWriteService asyncWriteService;

    @Test
    void saveTask_ShouldCallTaskDAO() {
        Task task = new Task();
        task.setId(1L);
        when(taskDAO.save(any(Task.class))).thenReturn(task);

        asyncWriteService.saveTask(task);

        verify(taskDAO).save(task);
        verify(sseService).broadcast(eq("task-updated"), any(Task.class));
    }

    @Test
    void deleteTask_ShouldCallTaskDAOAndBroadcast() {
        Long taskId = 1L;

        asyncWriteService.deleteTask(taskId);

        verify(taskDAO).deleteById(taskId);
        verify(sseService).broadcast(eq("task-deleted"), eq(taskId));
    }

    @Test
    void moveTask_ShouldCallTaskDAOUpdatePosition() {
        Long taskId = 1L;
        Long laneId = 2L;
        Integer position = 0;
        TaskStatus status = TaskStatus.DONE;

        Task task = new Task();
        task.setId(taskId);
        when(taskDAO.shiftPositionsDown(laneId, status, position, taskId)).thenReturn(0);
        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        asyncWriteService.moveTask(taskId, status, laneId, position);

        verify(taskDAO).updatePosition(taskId, status, laneId, position);
        verify(sseService).broadcast(eq("task-updated"), any(Task.class));
    }

    @Test
    void moveTask_ShouldSkipShift_WhenPositionIsNull() {
        Long taskId = 1L;
        Long laneId = 2L;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        Task task = new Task();
        task.setId(taskId);
        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        asyncWriteService.moveTask(taskId, status, laneId, null);

        verify(taskDAO, never()).shiftPositionsDown(any(), any(), any(), any());
        verify(taskDAO).updatePosition(taskId, status, laneId, null);
        verify(sseService).broadcast(eq("task-updated"), any(Task.class));
    }

    @Test
    void moveTask_ShouldSkipShift_WhenLaneIdIsNull() {
        Long taskId = 1L;
        Integer position = 0;
        TaskStatus status = TaskStatus.TODO;

        Task task = new Task();
        task.setId(taskId);
        when(taskDAO.findById(taskId)).thenReturn(Optional.of(task));

        asyncWriteService.moveTask(taskId, status, null, position);

        verify(taskDAO, never()).shiftPositionsDown(any(), any(), any(), any());
        verify(taskDAO).updatePosition(taskId, status, null, position);
        verify(sseService).broadcast(eq("task-updated"), any(Task.class));
    }

    @Test
    void moveTask_ShouldNotBroadcast_WhenTaskNotFound() {
        Long taskId = 999L;
        Long laneId = 2L;
        Integer position = 0;
        TaskStatus status = TaskStatus.DONE;

        when(taskDAO.shiftPositionsDown(laneId, status, position, taskId)).thenReturn(0);
        when(taskDAO.findById(taskId)).thenReturn(Optional.empty());

        asyncWriteService.moveTask(taskId, status, laneId, position);

        verify(taskDAO).updatePosition(taskId, status, laneId, position);
        verify(sseService, never()).broadcast(eq("task-updated"), any(Task.class));
    }

    @Test
    void saveSwimLane_ShouldCallSwimLaneDAO() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.save(any(SwimLane.class))).thenReturn(lane);

        asyncWriteService.saveSwimLane(lane);

        verify(swimLaneDAO).save(lane);
        verify(sseService).broadcast(eq("lane-updated"), any(SwimLane.class));
    }

    @Test
    void deleteSwimLane_ShouldCallSwimLaneDAOAndBroadcast() {
        Long laneId = 1L;

        asyncWriteService.deleteSwimLane(laneId);

        verify(swimLaneDAO).deleteById(laneId);
        verify(sseService).broadcast(eq("lane-updated"), any(SwimLane.class));
    }
}
