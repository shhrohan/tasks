package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.dao.TaskDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class AsyncWriteService {

    private final TaskDAO taskDAO;
    private final SwimLaneDAO swimLaneDAO;
    private final SseService sseService;

    public AsyncWriteService(TaskDAO taskDAO, SwimLaneDAO swimLaneDAO, SseService sseService) {
        this.taskDAO = taskDAO;
        this.swimLaneDAO = swimLaneDAO;
        this.sseService = sseService;
    }

    @Async("asyncWriteExecutor")
    @Transactional
    public void saveTask(Task task) {
        log.info("AsyncDB: Start processing SAVE for Task ID {} [Thread: {}]...", task.getId(),
                Thread.currentThread().getName());
        simulateLatency();
        Task savedTask = taskDAO.save(task);
        log.info("AsyncDB: Completed SAVE for Task ID {} [Thread: {}]", task.getId(), Thread.currentThread().getName());
        sseService.broadcast("task-updated", savedTask);
    }

    @Async("asyncWriteExecutor")
    @Transactional
    public void moveTask(Long id, com.example.todo.model.TaskStatus status, Long laneId) {
        log.info("AsyncDB: [DEBUG] Start processing MOVE for Task ID {} to Status {} Lane {} [Thread: {}]...", id,
                status, laneId, Thread.currentThread().getName());
        simulateLatency();
        taskDAO.updatePosition(id, status, laneId);

        // Fetch updated task to broadcast full state
        taskDAO.findById(id).ifPresent(task -> {
            log.info("AsyncDB: [DEBUG] Task found after update: ID={}, Name={}, Status={}, Lane={}",
                    task.getId(), task.getName(), task.getStatus(),
                    task.getSwimLane() != null ? task.getSwimLane().getId() : "null");
            sseService.broadcast("task-updated", task);
        });

        log.info("AsyncDB: Completed MOVE for Task ID {} [Thread: {}]", id, Thread.currentThread().getName());
    }

    @Async("asyncWriteExecutor")
    @Transactional
    public void deleteTask(Long id) {
        log.info("AsyncDB: Start processing DELETE for Task ID {} [Thread: {}]...", id,
                Thread.currentThread().getName());
        simulateLatency();
        taskDAO.deleteById(id);
        log.info("AsyncDB: Completed DELETE for Task ID {} [Thread: {}]", id, Thread.currentThread().getName());
        sseService.broadcast("task-deleted", id);
    }

    @Async("asyncWriteExecutor")
    @Transactional
    public void saveSwimLane(SwimLane lane) {
        log.info("AsyncDB: Start processing SAVE for SwimLane ID {} [Thread: {}]...", lane.getId(),
                Thread.currentThread().getName());
        simulateLatency();
        SwimLane savedLane = swimLaneDAO.save(lane);
        log.info("AsyncDB: Completed SAVE for SwimLane ID {} [Thread: {}]", lane.getId(),
                Thread.currentThread().getName());
        sseService.broadcast("lane-updated", savedLane);
    }

    @Async("asyncWriteExecutor")
    @Transactional
    public void deleteSwimLane(Long id) {
        log.info("AsyncDB: Start processing DELETE for SwimLane ID {} [Thread: {}]...", id,
                Thread.currentThread().getName());
        simulateLatency();
        swimLaneDAO.deleteById(id); // Note: Service calls this with ID, but original code might have been object.
        // Checking original code: deleteSwimLane(Long id) calls
        // swimLaneDAO.deleteById(id).
        // Wait, previous view showed deleteSwimLane(Long id).
        // Let's stick to the signature in the file.
        log.info("AsyncDB: Completed DELETE for SwimLane ID {} [Thread: {}]", id, Thread.currentThread().getName());
        sseService.broadcast("lane-updated", SwimLane.builder().id(id).isDeleted(true).build()); // Broadcast deletion
    }

    private void simulateLatency() {
        try {
            Thread.sleep(100); // Simulate network/DB latency to prove decoupling
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
