package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    @JsonBackReference
    private AssessmentSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    @Column(name = "feedback_text", columnDefinition = "TEXT", nullable = false)
    private String feedbackText;

    @Column(name = "user_responses_summary", columnDefinition = "TEXT")
    private String userResponsesSummary;

    @Column(name = "ai_model_used")
    private String aiModelUsed;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "skill_graph_data", columnDefinition = "JSON")
    private String skillGraphData;

    @Column(name = "behavioral_insights", columnDefinition = "JSON")
    private String behavioralInsights;

    @Column(name = "previous_feedback_actions", columnDefinition = "TEXT")
    private String previousFeedbackActions;

    @Column(name = "current_session_goal", columnDefinition = "TEXT")
    private String currentSessionGoal;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
