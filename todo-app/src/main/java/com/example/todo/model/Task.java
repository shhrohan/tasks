package com.example.todo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "position")
    private Integer position;

    // Store comments and tags as JSON strings (simple approach)
    @Lob
    private String comments; // JSON array string

    @Lob
    private String tags; // JSON array string

    @ManyToOne
    @JoinColumn(name = "swim_lane_id")
    private SwimLane swimLane;

    // Position within the column (for ordering)
    @Column(name = "position_order")
    private Integer position;
}
