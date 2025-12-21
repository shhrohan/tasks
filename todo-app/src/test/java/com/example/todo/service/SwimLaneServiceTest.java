package com.example.todo.service;

import com.example.todo.dao.SwimLaneDAO;
import com.example.todo.model.SwimLane;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
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

    @Mock
    private UserRepository userRepository;

    private SwimLaneService swimLaneService;

    private User testUser;

    @BeforeEach
    void setUp() {
        swimLaneService = new SwimLaneService(swimLaneDAO, asyncWriteService, userRepository);
        
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        // Mock SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test@example.com", null, Collections.emptyList());
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Mock UserRepository to return test user (lenient to avoid UnnecessaryStubbingException)
        lenient().when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSwimLane_ShouldSaveLane() {
        SwimLane lane = new SwimLane();
        lane.setName("New Lane");

        when(swimLaneDAO.findMaxPositionByUserId(testUser.getId())).thenReturn(5);
        when(swimLaneDAO.save(any(SwimLane.class))).thenReturn(lane);

        SwimLane result = swimLaneService.createSwimLane(lane);

        assertNotNull(result);
        assertEquals("New Lane", result.getName());
        assertEquals(6, result.getPosition()); // maxPos + 1
        assertEquals(testUser, result.getUser());
        verify(swimLaneDAO).save(lane);
    }

    @Test
    void createSwimLane_ShouldSetPositionToZero_WhenNoExistingLanes() {
        SwimLane lane = new SwimLane();
        lane.setName("First Lane");

        when(swimLaneDAO.findMaxPositionByUserId(testUser.getId())).thenReturn(null);
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
        lane.setUser(testUser);

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
    void completeSwimLane_ShouldThrowException_WhenNotOwned() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        User otherUser = new User();
        otherUser.setId(999L);
        lane.setUser(otherUser);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        assertThrows(RuntimeException.class, () -> swimLaneService.completeSwimLane(laneId));
    }

    @Test
    void deleteSwimLane_ShouldSoftDelete() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setIsDeleted(false);
        lane.setUser(testUser);

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
    void deleteSwimLane_ShouldThrowException_WhenNotOwned() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        User otherUser = new User();
        otherUser.setId(999L);
        lane.setUser(otherUser);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        assertThrows(RuntimeException.class, () -> swimLaneService.deleteSwimLane(laneId));
    }

    @Test
    void getAllSwimLanes_ShouldReturnAllLanesForUser() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        lane.setUser(testUser);
        when(swimLaneDAO.findByUserIdAndIsDeletedFalseOrderByPositionAsc(testUser.getId()))
                .thenReturn(Arrays.asList(lane));

        List<SwimLane> result = swimLaneService.getAllSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void getActiveSwimLanes_ShouldReturnActiveLanesForUser() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        lane.setUser(testUser);
        when(swimLaneDAO.findByUserIdAndIsCompletedFalseAndIsDeletedFalseOrderByPositionAsc(testUser.getId()))
                .thenReturn(Arrays.asList(lane));

        List<SwimLane> result = swimLaneService.getActiveSwimLanes();

        assertEquals(1, result.size());
        assertEquals(lane, result.get(0));
    }

    @Test
    void getCompletedSwimLanes_ShouldReturnCompletedLanesForUser() {
        SwimLane lane = new SwimLane();
        lane.setId(1L);
        lane.setUser(testUser);
        when(swimLaneDAO.findByUserIdAndIsCompletedTrueAndIsDeletedFalseOrderByPositionAsc(testUser.getId()))
                .thenReturn(Arrays.asList(lane));

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
        lane.setUser(testUser);

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
    void uncompleteSwimLane_ShouldThrowException_WhenNotOwned() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        User otherUser = new User();
        otherUser.setId(999L);
        lane.setUser(otherUser);

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

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
        lane1.setUser(testUser);

        SwimLane lane2 = new SwimLane();
        lane2.setId(2L);
        lane2.setPosition(1);
        lane2.setUser(testUser);

        SwimLane lane3 = new SwimLane();
        lane3.setId(3L);
        lane3.setPosition(2);
        lane3.setUser(testUser);

        List<Long> orderedIds = Arrays.asList(3L, 1L, 2L);
        when(swimLaneDAO.findAllById(orderedIds)).thenReturn(Arrays.asList(lane1, lane2, lane3));

        swimLaneService.reorderSwimLanes(orderedIds);

        // After reorder: 3L=0, 1L=1, 2L=2
        assertEquals(1, lane1.getPosition()); // id=1 is at index 1
        assertEquals(2, lane2.getPosition()); // id=2 is at index 2
        assertEquals(0, lane3.getPosition()); // id=3 is at index 0
        verify(swimLaneDAO).saveAll(anyList());
    }

    @Test
    void reorderSwimLanes_ShouldHandleMissingLanes() {
        SwimLane lane1 = new SwimLane();
        lane1.setId(1L);
        lane1.setPosition(0);
        lane1.setUser(testUser);

        List<Long> orderedIds = Arrays.asList(999L, 1L);
        when(swimLaneDAO.findAllById(orderedIds)).thenReturn(Arrays.asList(lane1));

        swimLaneService.reorderSwimLanes(orderedIds);

        assertEquals(1, lane1.getPosition());
        verify(swimLaneDAO).saveAll(anyList());
    }

    @Test
    void reorderSwimLanes_ShouldFilterOutOtherUsersLanes() {
        SwimLane myLane = new SwimLane();
        myLane.setId(1L);
        myLane.setPosition(0);
        myLane.setUser(testUser);

        SwimLane otherLane = new SwimLane();
        otherLane.setId(2L);
        otherLane.setPosition(1);
        User otherUser = new User();
        otherUser.setId(999L);
        otherLane.setUser(otherUser);

        List<Long> orderedIds = Arrays.asList(2L, 1L);
        when(swimLaneDAO.findAllById(orderedIds)).thenReturn(Arrays.asList(myLane, otherLane));

        swimLaneService.reorderSwimLanes(orderedIds);

        // Only myLane should be reordered (index 1 in orderedIds)
        assertEquals(1, myLane.getPosition());
        verify(swimLaneDAO).saveAll(anyList());
    }

    @Test
    void completeSwimLane_ShouldThrowException_WhenOwnerIsNull() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setUser(null); // No owner

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        assertThrows(RuntimeException.class, () -> swimLaneService.completeSwimLane(laneId));
    }

    @Test
    void uncompleteSwimLane_ShouldThrowException_WhenOwnerIsNull() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setUser(null); // No owner

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        assertThrows(RuntimeException.class, () -> swimLaneService.uncompleteSwimLane(laneId));
    }

    @Test
    void deleteSwimLane_ShouldThrowException_WhenOwnerIsNull() {
        Long laneId = 1L;
        SwimLane lane = new SwimLane();
        lane.setId(laneId);
        lane.setUser(null); // No owner

        when(swimLaneDAO.findById(laneId)).thenReturn(Optional.of(lane));

        assertThrows(RuntimeException.class, () -> swimLaneService.deleteSwimLane(laneId));
    }

    @Test
    void getCurrentUser_ShouldThrowException_WhenNoAuthentication() {
        // Clear the security context to simulate no authentication
        SecurityContextHolder.clearContext();
        SecurityContext emptyContext = mock(SecurityContext.class);
        when(emptyContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(emptyContext);

        assertThrows(RuntimeException.class, () -> swimLaneService.getAllSwimLanes());
    }

    @Test
    void getCurrentUser_ShouldThrowException_WhenUserNotFound() {
        // Override the mock to return empty
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> swimLaneService.getAllSwimLanes());
    }
    @Test
    void reorderSwimLanes_ShouldFilterOutLanesWithNullUser() {
        SwimLane myLane = new SwimLane();
        myLane.setId(1L);
        myLane.setPosition(0);
        myLane.setUser(testUser);

        SwimLane laneWithNoUser = new SwimLane();
        laneWithNoUser.setId(2L);
        laneWithNoUser.setPosition(1);
        laneWithNoUser.setUser(null);

        List<Long> orderedIds = Arrays.asList(2L, 1L);
        when(swimLaneDAO.findAllById(orderedIds)).thenReturn(Arrays.asList(myLane, laneWithNoUser));

        swimLaneService.reorderSwimLanes(orderedIds);

        // Only myLane should be reordered
        assertEquals(1, myLane.getPosition());
        verify(swimLaneDAO).saveAll(anyList());
    }

    @Test
    void getCurrentUser_ShouldThrowException_WhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        SecurityContext customContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken unauth = mock(UsernamePasswordAuthenticationToken.class);
        when(unauth.isAuthenticated()).thenReturn(false);
        when(customContext.getAuthentication()).thenReturn(unauth);
        SecurityContextHolder.setContext(customContext);

        assertThrows(RuntimeException.class, () -> swimLaneService.getAllSwimLanes());
    }
}


