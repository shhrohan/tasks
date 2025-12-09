package com.example.todo.dao;

import com.example.todo.model.SwimLane;
import com.example.todo.repository.SwimLaneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SwimLaneDAOTest {

    @Mock
    private SwimLaneRepository swimLaneRepository;

    @InjectMocks
    private SwimLaneDAO swimLaneDAO;

    @Test
    void findByIsDeletedFalse_ShouldDelegateToRepository() {
        when(swimLaneRepository.findByIsDeletedFalseOrderByPositionAsc()).thenReturn(Collections.emptyList());

        List<SwimLane> result = swimLaneDAO.findByIsDeletedFalseOrderByPositionAsc();

        assertNotNull(result);
        verify(swimLaneRepository).findByIsDeletedFalseOrderByPositionAsc();
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
        when(swimLaneRepository.findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc())
                .thenReturn(Collections.emptyList());

        List<SwimLane> result = swimLaneDAO.findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc();

        assertNotNull(result);
        verify(swimLaneRepository).findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc();
    }

    @Test
    void findByIsCompletedTrueAndIsDeletedFalse_ShouldDelegateToRepository() {
        when(swimLaneRepository.findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc())
                .thenReturn(Collections.emptyList());

        List<SwimLane> result = swimLaneDAO.findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc();

        assertNotNull(result);
        verify(swimLaneRepository).findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc();
    }

    @Test
    void findById_ShouldDelegateToRepository() {
        Long id = 1L;
        SwimLane lane = new SwimLane();
        when(swimLaneRepository.findById(id)).thenReturn(Optional.of(lane));

        Optional<SwimLane> result = swimLaneDAO.findById(id);

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

    @Test
    void findMaxPosition_ShouldDelegateToRepository() {
        when(swimLaneRepository.findMaxPosition()).thenReturn(5);

        Integer result = swimLaneDAO.findMaxPosition();

        assertEquals(5, result);
        verify(swimLaneRepository).findMaxPosition();
    }

    @Test
    void findMaxPosition_ShouldReturnNull_WhenNoLanes() {
        when(swimLaneRepository.findMaxPosition()).thenReturn(null);

        Integer result = swimLaneDAO.findMaxPosition();

        assertNull(result);
        verify(swimLaneRepository).findMaxPosition();
    }

    @Test
    void saveAll_ShouldDelegateToRepository() {
        SwimLane lane1 = new SwimLane();
        lane1.setId(1L);
        SwimLane lane2 = new SwimLane();
        lane2.setId(2L);
        List<SwimLane> lanes = Arrays.asList(lane1, lane2);

        when(swimLaneRepository.saveAll(lanes)).thenReturn(lanes);

        List<SwimLane> result = swimLaneDAO.saveAll(lanes);

        assertEquals(2, result.size());
        verify(swimLaneRepository).saveAll(lanes);
    }

    @Test
    void findAllById_ShouldDelegateToRepository() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        SwimLane lane1 = new SwimLane();
        lane1.setId(1L);
        SwimLane lane2 = new SwimLane();
        lane2.setId(2L);

        when(swimLaneRepository.findAllById(ids)).thenReturn(Arrays.asList(lane1, lane2));

        List<SwimLane> result = swimLaneDAO.findAllById(ids);

        assertEquals(2, result.size());
        verify(swimLaneRepository).findAllById(ids);
    }
}
