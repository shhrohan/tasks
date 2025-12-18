package com.example.todo.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @Lob
    private String tags; // JSON array string

    @ManyToOne
    @JoinColumn(name = "swim_lane_id")
    private SwimLane swimLane;

    // Position within the column (for ordering)
    @Column(name = "position_order")
    private Integer position;
}
