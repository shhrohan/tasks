package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.model.SwimLane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

        when(swimLaneDAO.findMaxPosition()).thenReturn(5);
        when(swimLaneDAO.save(any(SwimLane.class))).thenReturn(lane);

        SwimLane result = swimLaneService.createSwimLane(lane);

        assertNotNull(result);
        assertEquals("New Lane", result.getName());
        assertEquals(6, result.getPosition()); // maxPos + 1
        verify(swimLaneDAO).save(lane);
    }

    @Test
    void createSwimLane_ShouldSetPositionToZero_WhenNoExistingLanes() {
        SwimLane lane = new SwimLane();
        lane.setName("First Lane");

        when(swimLaneDAO.findMaxPosition()).thenReturn(null);
        when(swimLaneDAO.save(any(SwimLane.class))).thenReturn(lane);

        SwimLane result = swimLaneService.createSwimLane(lane);

        assertEquals(0, result.getPosition());
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
    void completeSwimLane_ShouldThrowException_WhenNotFound() {
        Long laneId = 999L;
        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> swimLaneService.completeSwimLane(laneId));
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
    void deleteSwimLane_ShouldThrowException_WhenNotFound() {
        Long laneId = 999L;
        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> swimLaneService.deleteSwimLane(laneId));
    }

    @Test
    void getAllSwimLanes_ShouldReturnAllLanes() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.findByIsDeletedFalseOrderByPositionAsc()).thenReturn(Arrays.asList(lane));

        List<SwimLane> result = swimLaneService.getAllSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void getActiveSwimLanes_ShouldReturnActiveLanes() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.findByIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc()).thenReturn(Arrays.asList(lane));

        List<SwimLane> result = swimLaneService.getActiveSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void getCompletedSwimLanes_ShouldReturnCompletedLanes() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        when(swimLaneDAO.findByIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc()).thenReturn(Arrays.asList(lane));

        List<SwimLane> result = swimLaneService.getCompletedSwimLanes();

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
    void uncompleteSwimLane_ShouldThrowException_WhenNotFound() {
        Long laneId = 999L;
        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> swimLaneService.uncompleteSwimLane(laneId));
    }

    @Test
    void hardDeleteSwimLane_ShouldCallDeleteById() {
        Long laneId = 1L;
        doNothing().when(swimLaneDAO).deleteById(laneId);

        swimLaneService.hardDeleteSwimLane(laneId);

        verify(swimLaneDAO).deleteById(laneId);
    }

    @Test
    void reorderSwimLanes_ShouldUpdatePositions() {
        SwimLane lane1 = new SwimLane();
        lane1.setId(1L);
        lane1.setPosition(0);

        SwimLane lane2 = new SwimLane();
        lane2.setId(2L);
        lane2.setPosition(1);

        SwimLane lane3 = new SwimLane();
        lane3.setId(3L);
        lane3.setPosition(2);

        List<Long> orderedIds = Arrays.asList(3L, 1L, 2L);
        when(swimLaneDAO.findAllById(orderedIds)).thenReturn(Arrays.asList(lane1, lane2, lane3));

        swimLaneService.reorderSwimLanes(orderedIds);

        // After reorder: 3L=0, 1L=1, 2L=2
        assertEquals(1, lane1.getPosition()); // id=1 is at index 1
        assertEquals(2, lane2.getPosition()); // id=2 is at index 2
        assertEquals(0, lane3.getPosition()); // id=3 is at index 0
        verify(swimLaneDAO).saveAll(anyList());
    }
}
