package com.example.todo.repository;

import com.example.todo.model.SwimLane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SwimLaneRepository extends JpaRepository<SwimLane, Long> {
    List<SwimLane> findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc();
    List<SwimLane> findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc();
    List<SwimLane> findByIsDeletedFalseOrderByPositionAsc();
    
    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.position) FROM SwimLane s")
    Integer findMaxPosition();
}
