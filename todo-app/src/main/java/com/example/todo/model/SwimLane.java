package com.example.todo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "swim_lanes", indexes = {
    @Index(name = "idx_swim_lanes_user_id", columnList = "user_id"),
    @Index(name = "idx_swim_lanes_user_deleted_position", columnList = "user_id, is_deleted, position_order"),
    @Index(name = "idx_swim_lanes_user_completed_deleted", columnList = "user_id, is_completed, is_deleted, position_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwimLane {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "position_order")
    private Integer position;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}


