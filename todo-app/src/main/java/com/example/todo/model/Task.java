package com.example.todo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_swim_lane_id", columnList = "swim_lane_id"),
    @Index(name = "idx_tasks_status", columnList = "status"),
    @Index(name = "idx_tasks_lane_status_position", columnList = "swim_lane_id, status, position_order")
})
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

