package com.example.todo.dao;

import com.example.todo.model.SwimLane;
import com.example.todo.repository.SwimLaneRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SwimLaneDAO {

    private final SwimLaneRepository swimLaneRepository;

    public SwimLaneDAO(SwimLaneRepository swimLaneRepository) {
        this.swimLaneRepository = swimLaneRepository;
    }

    public List<SwimLane> findByIsDeletedFalse() {
        return swimLaneRepository.findByIsDeletedFalse();
    }

    public List<SwimLane> findByIsCompletedFalseAndIsDeletedFalse() {
        return swimLaneRepository.findByIsCompletedFalseAndIsDeletedFalse();
    }

    public List<SwimLane> findByIsCompletedTrueAndIsDeletedFalse() {
        return swimLaneRepository.findByIsCompletedTrueAndIsDeletedFalse();
    }

    public Optional<SwimLane> findById(Long id) {
        return swimLaneRepository.findById(id);
    }

    public SwimLane save(SwimLane swimLane) {
        return swimLaneRepository.save(swimLane);
    }

    public void deleteById(Long id) {
        swimLaneRepository.deleteById(id);
    }
}
