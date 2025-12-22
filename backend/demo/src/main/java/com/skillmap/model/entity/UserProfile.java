package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    // Learning preferences
    @Column(name = "preferred_question_types", columnDefinition = "JSON")
    private String preferredQuestionTypes; // JSON array of preferred question types

    @Column(name = "preferred_difficulty_range")
    private String preferredDifficultyRange; // e.g., "0.3-0.7"

    @Column(name = "learning_style")
    private String learningStyle; // visual, kinesthetic, auditory, reading

    @Column(name = "time_preference")
    private String timePreference; // morning, afternoon, evening

    // Skill progression data
    @Column(name = "skill_progression_data", columnDefinition = "JSON")
    private String skillProgressionData; // Historical skill level changes

    @Column(name = "cognitive_bias_trends", columnDefinition = "JSON")
    private String cognitiveBiasTrends; // Trends in cognitive biases over time

    @Column(name = "performance_patterns", columnDefinition = "JSON")
    private String performancePatterns; // Patterns in performance metrics

    // Adaptive assessment settings
    @Column(name = "adaptive_enabled", nullable = false)
    private Boolean adaptiveEnabled = true;

    @Column(name = "difficulty_adjustment_rate")
    private Double difficultyAdjustmentRate = 0.1; // How quickly to adjust difficulty

    @Column(name = "question_type_weights", columnDefinition = "JSON")
    private String questionTypeWeights; // Weights for different question types

    // Session statistics
    @Column(name = "total_sessions_completed")
    private Integer totalSessionsCompleted = 0;

    @Column(name = "average_session_duration_minutes")
    private Double averageSessionDurationMinutes;

    @Column(name = "preferred_session_length_minutes")
    private Integer preferredSessionLengthMinutes;

    @Column(name = "last_assessment_date")
    private LocalDateTime lastAssessmentDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
