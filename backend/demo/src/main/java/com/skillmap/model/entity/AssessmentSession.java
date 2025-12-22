package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assessment_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_token", unique = true, nullable = false)
    private String sessionToken;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "resume_uploaded")
    private Boolean resumeUploaded;

    @Column(name = "resume_extracted_skills", columnDefinition = "JSON")
    private String resumeExtractedSkills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "questions_asked")
    private Integer questionsAsked;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Response> responses;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SkillAssessment> skillAssessments;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Roadmap> roadmaps;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ResumeData> resumeData;

    public enum Status {
        in_progress, completed, abandoned
    }

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}
