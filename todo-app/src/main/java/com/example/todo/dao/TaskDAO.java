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

    public void updatePosition(Long id, com.example.todo.model.TaskStatus status, Long laneId, Integer position) {
        taskRepository.updatePosition(id, status, laneId, position);
    }

    public List<Task> findBySwimLaneId(Long swimLaneId) {
        return taskRepository.findBySwimLaneId(swimLaneId);
    }

    /**
     * Shift positions of existing tasks to make room for a task at the target
     * position.
     * Returns the number of tasks shifted.
     */
    public int shiftPositionsDown(Long laneId, com.example.todo.model.TaskStatus status, Integer targetPosition,
            Long excludeTaskId) {
        return taskRepository.shiftPositionsDown(laneId, status, targetPosition, excludeTaskId);
    }
}
