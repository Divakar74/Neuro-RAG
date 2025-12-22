package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    @JsonBackReference
    private Skill skill;

    @Column(name = "target_level", nullable = false)
    private Integer targetLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(nullable = false)
    private String title;

    @Column
    private String provider;

    @Column
    private String url;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Enumerated(EnumType.STRING)
    @Column
    private Difficulty difficulty;

    @Column(columnDefinition = "DECIMAL(2,1)")
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "DECIMAL(8,2)")
    private BigDecimal price;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum ResourceType {
        course, book, tutorial, project, certification
    }

    public enum Difficulty {
        beginner, intermediate, advanced
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
