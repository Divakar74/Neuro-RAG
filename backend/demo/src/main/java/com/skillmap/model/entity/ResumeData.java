package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeData {

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

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(name = "extracted_skills", columnDefinition = "JSON")
    private String extractedSkills;

    @Column(name = "extracted_education", columnDefinition = "JSON")
    private String extractedEducation;

    @Column(name = "extracted_experience", columnDefinition = "JSON")
    private String extractedExperience;

    @Column(name = "total_years_experience")
    private Integer totalYearsExperience;

    @Column(name = "raw_entities", columnDefinition = "JSON")
    private String rawEntities;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @PrePersist
    protected void onCreate() {
        parsedAt = LocalDateTime.now();
    }
}
