package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "roadmaps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonBackReference
    private AssessmentSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    @Column(name = "current_overall_level", columnDefinition = "DECIMAL(3,2)")
    private Double currentOverallLevel;

    @Column(name = "target_level")
    private Integer targetLevel;

    @Column(name = "level_label")
    private String levelLabel;

    @Column(name = "gap_analysis", columnDefinition = "JSON")
    private String gapAnalysis;

    @Column(name = "milestones", columnDefinition = "JSON")
    private String milestones;

    @Column(name = "total_estimated_weeks")
    private Integer totalEstimatedWeeks;

    @Column(name = "motivational_message", columnDefinition = "TEXT")
    private String motivationalMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
