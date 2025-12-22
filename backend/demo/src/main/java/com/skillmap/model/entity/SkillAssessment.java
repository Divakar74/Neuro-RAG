package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_assessments",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"session_id", "skill_id"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssessment {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    @JsonBackReference
    private Skill skill;

    @Column(name = "assessed_level", columnDefinition = "DECIMAL(3,2)", nullable = false)
    private Double assessedLevel;

    @Column(name = "confidence_score", columnDefinition = "DECIMAL(3,2)", nullable = false)
    private Double confidenceScore;

    @Column(name = "evidence_response_ids", columnDefinition = "JSON")
    private String evidenceResponseIds;

    @Column(name = "consistency_score", columnDefinition = "DECIMAL(3,2)")
    private Double consistencyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "depth_rating")
    private DepthRating depthRating;

    @Column(name = "behavioral_score", columnDefinition = "DECIMAL(3,2)")
    private Double behavioralScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum DepthRating {
        surface, moderate, deep
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
