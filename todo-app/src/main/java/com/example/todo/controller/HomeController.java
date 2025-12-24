package com.example.todo.controller;

import com.example.todo.model.SwimLane;
import com.example.todo.service.SwimLaneService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.todo.repository.UserRepository;
import com.example.todo.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * HomeController - Serves the main index page with initial data.
 * 
 * PERFORMANCE OPTIMIZATION:
 * Only sends lanes on initial load. Tasks are lazy-loaded when lanes are expanded.
 * This reduces initial payload and speeds up time-to-interactive.
 * 
 * Caches used:
 * - lanes: Swimlanes per user
 * - tasksByLane: Tasks fetched on-demand when lane is expanded
 */
@Controller
@Log4j2
public class HomeController {

    private final SwimLaneService swimLaneService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public HomeController(SwimLaneService swimLaneService, ObjectMapper objectMapper,
            UserRepository userRepository) {
        this.swimLaneService = swimLaneService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model) throws JsonProcessingException {
        log.info("[HomeController] Serving index page with initial data");

        // Fetch only lanes (tasks are lazy-loaded when lane is expanded)
        List<SwimLane> lanes = swimLaneService.getActiveSwimLanes();

        log.info("[HomeController] Loaded {} lanes (tasks will be lazy-loaded)", lanes.size());

        // Create initial data object - NO tasks, only lanes
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("lanes", lanes);
        initialData.put("tasks", Collections.emptyList()); // Empty - lazy loaded

        // Add user info (safe subset)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(username).orElseThrow();
        Map<String, String> userSafe = new HashMap<>();
        userSafe.put("name", currentUser.getName());
        userSafe.put("firstName", currentUser.getName().split(" ")[0]);
        userSafe.put("email", currentUser.getEmail());
        userSafe.put("joinedAt", currentUser.getCreatedAt() != null ? currentUser.getCreatedAt().toString() : "N/A");
        initialData.put("user", userSafe);

        // Serialize to JSON and add to model
        String initialDataJson = objectMapper.writeValueAsString(initialData);
        model.addAttribute("initialDataJson", initialDataJson);

        return "index";
    }
}
