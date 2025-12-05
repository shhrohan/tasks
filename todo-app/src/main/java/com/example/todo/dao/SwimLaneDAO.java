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

    public List<SwimLane> findByIsDeletedFalseOrderByPositionAsc() {
        return swimLaneRepository.findByIsDeletedFalseOrderByPositionAsc();
    }

    public List<SwimLane> findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc() {
        return swimLaneRepository.findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc();
    }

    public List<SwimLane> findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc() {
        return swimLaneRepository.findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc();
    }

    public Integer findMaxPosition() {
        return swimLaneRepository.findMaxPosition();
    }

    public Optional<SwimLane> findById(Long id) {
        return swimLaneRepository.findById(id);
    }

    public SwimLane save(SwimLane swimLane) {
        return swimLaneRepository.save(swimLane);
    }

    public List<SwimLane> saveAll(List<SwimLane> swimLanes) {
        return swimLaneRepository.saveAll(swimLanes);
    }

    public List<SwimLane> findAllById(List<Long> ids) {
        return swimLaneRepository.findAllById(ids);
    }

    public void deleteById(Long id) {
        swimLaneRepository.deleteById(id);
    }
}
