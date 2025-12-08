package com.example.todo.repository;

import com.example.todo.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

        /**
         * Update a single task's status, lane, and position
         */
        @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
        @org.springframework.data.jpa.repository.Query("UPDATE Task t SET t.status = :status, t.swimLane.id = :laneId, t.position = :position WHERE t.id = :id")
        void updatePosition(@org.springframework.data.repository.query.Param("id") Long id,
                        @org.springframework.data.repository.query.Param("status") com.example.todo.model.TaskStatus status,
                        @org.springframework.data.repository.query.Param("laneId") Long laneId,
                        @org.springframework.data.repository.query.Param("position") Integer position);

        /**
         * BULK UPDATE: Shift all tasks at position >= targetPosition down by 1
         * (increment their positions)
         * This is called BEFORE inserting a task at targetPosition to make room.
         * Only affects tasks in the same lane AND status column.
         * Excludes the task being moved (excludeTaskId).
         */
        @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
        @org.springframework.data.jpa.repository.Query("UPDATE Task t SET t.position = t.position + 1 " +
                        "WHERE t.swimLane.id = :laneId " +
                        "AND t.status = :status " +
                        "AND t.position >= :targetPosition " +
                        "AND t.position IS NOT NULL " +
                        "AND t.id != :excludeTaskId")
        int shiftPositionsDown(
                        @org.springframework.data.repository.query.Param("laneId") Long laneId,
                        @org.springframework.data.repository.query.Param("status") com.example.todo.model.TaskStatus status,
                        @org.springframework.data.repository.query.Param("targetPosition") Integer targetPosition,
                        @org.springframework.data.repository.query.Param("excludeTaskId") Long excludeTaskId);

        /**
         * Fetch tasks for a lane, ordered by position (nulls last)
         */
        @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.swimLane.id = :swimLaneId ORDER BY t.position ASC NULLS LAST")
        java.util.List<Task> findBySwimLaneId(
                        @org.springframework.data.repository.query.Param("swimLaneId") Long swimLaneId);
}
