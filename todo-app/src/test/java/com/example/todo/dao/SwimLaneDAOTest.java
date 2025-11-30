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
}
