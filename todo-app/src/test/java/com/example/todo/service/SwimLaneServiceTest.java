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
        when(swimLaneDAO.save(any(SwimLane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SwimLane result = swimLaneService.completeSwimLane(laneId);

        assertTrue(result.getIsCompleted());
        verify(swimLaneDAO).save(lane);
    }

    @Test
    void deleteSwimLane_ShouldSoftDelete() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setIsDeleted(false);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));
        when(swimLaneDAO.save(any(SwimLane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        swimLaneService.deleteSwimLane(laneId);

        assertTrue(lane.getIsDeleted());
        verify(swimLaneDAO).save(lane);
    }
}
