package com.example.todo.dao;

import com.example.todo.model.Task;
import com.example.todo.repository.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TaskDAO {

    private final TaskRepository taskRepository;

    public TaskDAO(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public Task save(Task task) {
        return taskRepository.save(task);
    }

    public void deleteById(Long id) {
        taskRepository.deleteById(id);
    }

    public void updatePosition(Long id, com.example.todo.model.TaskStatus status, Long laneId) {
        taskRepository.updatePosition(id, status, laneId);
    }

    public List<Task> findBySwimLaneId(Long swimLaneId) {
        return taskRepository.findBySwimLaneId(swimLaneId);
    }
}
