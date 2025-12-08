package com.example.todo.dao;

import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDAOTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskDAO taskDAO;

    @Test
    void findById_ShouldDelegateToRepository() {
        Long id = 1L;
        Task task = new Task();
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        Optional<Task> result = taskDAO.findById(id);

        assertTrue(result.isPresent());
        verify(taskRepository).findById(id);
    }

    @Test
    void save_ShouldDelegateToRepository() {
        Task task = new Task();
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskDAO.save(task);

        assertEquals(task, result);
        verify(taskRepository).save(task);
    }

    @Test
    void findAll_ShouldDelegateToRepository() {
        Task task = new Task();
        when(taskRepository.findAll()).thenReturn(Arrays.asList(task));

        List<Task> result = taskDAO.findAll();

        assertEquals(1, result.size());
        assertEquals(task, result.get(0));
        verify(taskRepository).findAll();
    }

    @Test
    void deleteById_ShouldDelegateToRepository() {
        Long id = 1L;
        doNothing().when(taskRepository).deleteById(id);

        taskDAO.deleteById(id);

        verify(taskRepository).deleteById(id);
    }

    @Test
    void updatePosition_ShouldDelegateToRepository() {
        Long id = 1L;
        TaskStatus status = TaskStatus.IN_PROGRESS;
        Long laneId = 2L;
        Integer position = 3;

        doNothing().when(taskRepository).updatePosition(id, status, laneId, position);

        taskDAO.updatePosition(id, status, laneId, position);

        verify(taskRepository).updatePosition(id, status, laneId, position);
    }

    @Test
    void findBySwimLaneId_ShouldDelegateToRepository() {
        Long swimLaneId = 1L;
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(2L);
        when(taskRepository.findBySwimLaneId(swimLaneId)).thenReturn(Arrays.asList(task1, task2));

        List<Task> result = taskDAO.findBySwimLaneId(swimLaneId);

        assertEquals(2, result.size());
        verify(taskRepository).findBySwimLaneId(swimLaneId);
    }

    @Test
    void shiftPositionsDown_ShouldDelegateToRepository() {
        Long laneId = 1L;
        TaskStatus status = TaskStatus.TODO;
        Integer targetPosition = 2;
        Long excludeTaskId = 5L;
        int expectedShifted = 3;

        when(taskRepository.shiftPositionsDown(laneId, status, targetPosition, excludeTaskId))
                .thenReturn(expectedShifted);

        int result = taskDAO.shiftPositionsDown(laneId, status, targetPosition, excludeTaskId);

        assertEquals(expectedShifted, result);
        verify(taskRepository).shiftPositionsDown(laneId, status, targetPosition, excludeTaskId);
    }
}
