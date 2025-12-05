package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.model.SwimLane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwimLaneServiceTest {

    @Mock
    private SwimLaneDAO swimLaneDAO;

    @Mock
    private AsyncWriteService asyncWriteService;

    @InjectMocks
    private SwimLaneService swimLaneService;

    @Test
    void createSwimLane_ShouldSaveLane() {
        SwimLane lane = new SwimLane();
        lane.setName("New Lane");

        when(swimLaneDAO.save(any(SwimLane.class))).thenReturn(lane);

        SwimLane result = swimLaneService.createSwimLane(lane);

        assertNotNull(result);
        assertEquals("New Lane", result.getName());
        verify(swimLaneDAO).save(lane);
    }

    @Test
    void completeSwimLane_ShouldSetIsCompletedTrue() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setIsCompleted(false);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        SwimLane result = swimLaneService.completeSwimLane(laneId);

        assertTrue(result.getIsCompleted());
        verify(asyncWriteService).saveSwimLane(lane);
        verify(swimLaneDAO, never()).save(lane);
    }

    @Test
    void deleteSwimLane_ShouldSoftDelete() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setIsDeleted(false);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));
        swimLaneService.deleteSwimLane(laneId);

        assertTrue(lane.getIsDeleted());
        verify(asyncWriteService).saveSwimLane(lane);
        verify(swimLaneDAO, never()).save(lane);
    }

    @Test
    void getAllSwimLanes_ShouldReturnAllLanes() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.findByIsDeletedFalse()).thenReturn(java.util.Arrays.asList(lane));

        java.util.List<SwimLane> result = swimLaneService.getAllSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void getActiveSwimLanes_ShouldReturnActiveLanes() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.findByIsCompletedFalseAndIsDeletedFalse()).thenReturn(java.util.Arrays.asList(lane));

        java.util.List<SwimLane> result = swimLaneService.getActiveSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void getCompletedSwimLanes_ShouldReturnCompletedLanes() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.findByIsCompletedTrueAndIsDeletedFalse()).thenReturn(java.util.Arrays.asList(lane));

        java.util.List<SwimLane> result = swimLaneService.getCompletedSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void uncompleteSwimLane_ShouldSetIsCompletedFalse() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setIsCompleted(true);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        SwimLane result = swimLaneService.uncompleteSwimLane(laneId);

        assertFalse(result.getIsCompleted());
        verify(asyncWriteService).saveSwimLane(lane);
        verify(swimLaneDAO, never()).save(lane);
    }

    @Test
    void hardDeleteSwimLane_ShouldCallDeleteById() {
        Long laneId = 1L;
        doNothing().when(swimLaneDAO).deleteById(laneId);

        swimLaneService.hardDeleteSwimLane(laneId);

        verify(swimLaneDAO).deleteById(laneId);
    }
}
