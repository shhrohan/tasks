package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void deleteTask_ShouldCallTaskDAO() {
        Long taskId = 1L;

        asyncWriteService.deleteTask(taskId);

        verify(taskDAO).deleteById(taskId);
    }

    @Test
    void moveTask_ShouldCallTaskDAOUpdatePosition() {
        Long taskId = 1L;
        Long laneId = 2L;
        Integer position = 0;
        com.example.todo.model.TaskStatus status = com.example.todo.model.TaskStatus.DONE;

        Task task = new Task();
        task.setId(taskId);
        when(taskDAO.shiftPositionsDown(laneId, status, position, taskId)).thenReturn(0);
        when(taskDAO.findById(taskId)).thenReturn(java.util.Optional.of(task));

        asyncWriteService.moveTask(taskId, status, laneId, position);

        verify(taskDAO).updatePosition(taskId, status, laneId, position);
        verify(sseService).broadcast(eq("task-updated"), any(Task.class));
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
    void deleteSwimLane_ShouldCallSwimLaneDAO() {
        Long laneId = 1L;

        asyncWriteService.deleteSwimLane(laneId);

        verify(swimLaneDAO).deleteById(laneId);
    }
}
