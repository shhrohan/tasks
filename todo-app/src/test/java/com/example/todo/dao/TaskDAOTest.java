package com.example.todo.dao;

import com.example.todo.model.Task;
import com.example.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(taskRepository.findAll()).thenReturn(java.util.Arrays.asList(task));

        java.util.List<Task> result = taskDAO.findAll();

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
}
