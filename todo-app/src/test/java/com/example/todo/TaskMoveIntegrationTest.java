package com.example.todo;

import com.example.todo.base.BaseIntegrationTest;
import com.example.todo.model.SwimLane;
import com.example.todo.model.Task;
import com.example.todo.model.TaskStatus;
import com.example.todo.repository.SwimLaneRepository;
import com.example.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
public class TaskMoveIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SwimLaneRepository swimLaneRepository;

    @Test
    void shouldMoveTaskWithSwimLane() throws Exception {
        SwimLane lane1 = new SwimLane();
        lane1.setName("Lane 1");
        lane1 = swimLaneRepository.save(lane1);

        SwimLane lane2 = new SwimLane();
        lane2.setName("Lane 2");
        lane2 = swimLaneRepository.save(lane2);

        Task task = Task.builder()
                .name("Move Me")
                .status(TaskStatus.TODO)
                .swimLane(lane1)
                .build();
        task = taskRepository.save(task);

        mockMvc.perform(patch("/api/tasks/" + task.getId() + "/move")
                .param("status", "IN_PROGRESS")
                .param("swimLaneId", lane2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.swimLane.id", is(lane2.getId().intValue())));
    }
}
