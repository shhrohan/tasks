package com.example.todo.dao;

import com.example.todo.model.SwimLane;
import com.example.todo.repository.SwimLaneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwimLaneDAOTest {

    @Mock
    private SwimLaneRepository swimLaneRepository;

    @InjectMocks
    private SwimLaneDAO swimLaneDAO;

    @Test
    void findByIsDeletedFalse_ShouldDelegateToRepository() {
        when(swimLaneRepository.findByIsDeletedFalse()).thenReturn(Collections.emptyList());

        List<SwimLane> result = swimLaneDAO.findByIsDeletedFalse();

        assertNotNull(result);
        verify(swimLaneRepository).findByIsDeletedFalse();
    }

    @Test
    void save_ShouldDelegateToRepository() {
        SwimLane lane = new SwimLane();
        when(swimLaneRepository.save(lane)).thenReturn(lane);

        SwimLane result = swimLaneDAO.save(lane);

        assertEquals(lane, result);
        verify(swimLaneRepository).save(lane);
    }

    @Test
    void findByIsCompletedFalseAndIsDeletedFalse_ShouldDelegateToRepository() {
        when(swimLaneRepository.findByIsCompletedFalseAndIsDeletedFalse()).thenReturn(Collections.emptyList());

        List<SwimLane> result = swimLaneDAO.findByIsCompletedFalseAndIsDeletedFalse();

        assertNotNull(result);
        verify(swimLaneRepository).findByIsCompletedFalseAndIsDeletedFalse();
    }

    @Test
    void findByIsCompletedTrueAndIsDeletedFalse_ShouldDelegateToRepository() {
        when(swimLaneRepository.findByIsCompletedTrueAndIsDeletedFalse()).thenReturn(Collections.emptyList());

        List<SwimLane> result = swimLaneDAO.findByIsCompletedTrueAndIsDeletedFalse();

        assertNotNull(result);
        verify(swimLaneRepository).findByIsCompletedTrueAndIsDeletedFalse();
    }

    @Test
    void findById_ShouldDelegateToRepository() {
        Long id = 1L;
        SwimLane lane = new SwimLane();
        when(swimLaneRepository.findById(id)).thenReturn(java.util.Optional.of(lane));

        java.util.Optional<SwimLane> result = swimLaneDAO.findById(id);

        assertTrue(result.isPresent());
        verify(swimLaneRepository).findById(id);
    }

    @Test
    void deleteById_ShouldDelegateToRepository() {
        Long id = 1L;
        doNothing().when(swimLaneRepository).deleteById(id);

        swimLaneDAO.deleteById(id);

        verify(swimLaneRepository).deleteById(id);
    }
}
