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

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncWriteServiceTest {

    @Mock
    private TaskDAO taskDAO;

    @Mock
    private SwimLaneDAO swimLaneDAO;

    @InjectMocks
    private AsyncWriteService asyncWriteService;

    @Test
    void saveTask_ShouldCallTaskDAO() {
        Task task = new Task();
        task.setId(1L);

        asyncWriteService.saveTask(task);

        verify(taskDAO).save(task);
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
        com.example.todo.model.TaskStatus status = com.example.todo.model.TaskStatus.DONE;

        asyncWriteService.moveTask(taskId, status, laneId);

        verify(taskDAO).updatePosition(taskId, status, laneId);
    }

    @Test
    void saveSwimLane_ShouldCallSwimLaneDAO() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);

        asyncWriteService.saveSwimLane(lane);

        verify(swimLaneDAO).save(lane);
    }

    @Test
    void deleteSwimLane_ShouldCallSwimLaneDAO() {
        Long laneId = 1L;

        asyncWriteService.deleteSwimLane(laneId);

        verify(swimLaneDAO).deleteById(laneId);
    }
}
