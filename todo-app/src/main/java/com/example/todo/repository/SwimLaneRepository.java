package com.example.todo.repository;

import com.example.todo.model.SwimLane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SwimLaneRepository extends JpaRepository<SwimLane, Long> {
    // Legacy methods (without user filter) - keep for backward compatibility
    List<SwimLane> findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc();
    List<SwimLane> findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc();
    List<SwimLane> findByIsDeletedFalseOrderByPositionAsc();
    
    // User-filtered methods for data isolation
    List<SwimLane> findByUserIdAndIsDeletedFalseOrderByPositionAsc(Long userId);
    List<SwimLane> findByUserIdAndIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc(Long userId);
    List<SwimLane> findByUserIdAndIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc(Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.position) FROM SwimLane s WHERE s.user.id = :userId")
    Integer findMaxPositionByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.position) FROM SwimLane s")
    Integer findMaxPosition();
}

