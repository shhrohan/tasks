package com.example.todo.controller;

import com.example.todo.model.User;
import com.example.todo.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(@RequestParam(value = "error", required = false) String error,
                          Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        log.info("Registration attempt for email: {}", email);

        // Validation
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addAttribute("error", "Name is required");
            return "redirect:/register";
        }

        if (email == null || !email.contains("@")) {
            redirectAttributes.addAttribute("error", "Valid email is required");
            return "redirect:/register";
        }

        if (password == null || password.length() < 6) {
            redirectAttributes.addAttribute("error", "Password must be at least 6 characters");
            return "redirect:/register";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }

        // Check if email already exists
        if (userService.getUserByEmail(email).isPresent()) {
            redirectAttributes.addAttribute("error", "Email already registered");
            return "redirect:/register";
        }

        // Create user
        User newUser = User.builder()
                .name(name.trim())
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password))
                .build();

        userService.createUser(newUser);
        log.info("User registered successfully: {}", email);

        redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
        return "redirect:/login";
    }
}

