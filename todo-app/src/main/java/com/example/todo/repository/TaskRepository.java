package com.example.todo.repository;

import com.example.todo.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE Task t SET t.status = :status, t.swimLane.id = :laneId WHERE t.id = :id")
    void updatePosition(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("status") com.example.todo.model.TaskStatus status,
            @org.springframework.data.repository.query.Param("laneId") Long laneId);
}
