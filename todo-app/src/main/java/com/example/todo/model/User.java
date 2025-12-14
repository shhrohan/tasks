package com.example.todo.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * User entity for authentication and data ownership.
 * Designed to be extended for future login/auth features.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Future auth fields (commented out for now, will be enabled with login feature)
    // @Column
    // private String passwordHash;
    
    // @Column
    // private String avatarUrl;
    
    // @Column
    // private LocalDateTime createdAt;
    
    // @Column
    // private LocalDateTime lastLoginAt;
}
