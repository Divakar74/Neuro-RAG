package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;

@Entity
@Table(name = "responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {

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
    @JoinColumn(name = "question_id", nullable = false)
    @JsonBackReference
    private Question question;

    @Column(name = "response_text", columnDefinition = "TEXT", nullable = false)
    private String responseText;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "keyword_matches", columnDefinition = "JSON")
    private String keywordMatches;

    @Column(name = "think_time_seconds")
    private Integer thinkTimeSeconds;

    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds;

    @Column(name = "edit_count", columnDefinition = "INT DEFAULT 0")
    private Integer editCount;

    @Column(name = "paste_detected", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean pasteDetected;

    @Column(name = "specificity_score", columnDefinition = "DECIMAL(4,3)")
    private Double specificityScore;

    @Column(name = "depth_score", columnDefinition = "DECIMAL(4,3)")
    private Double depthScore;

    // Cognitive analysis fields
    @Column(name = "confidence_level", columnDefinition = "DECIMAL(3,2)")
    private Double confidenceLevel; // User-reported confidence (0.0 to 1.0)

    @Column(name = "consistency_score", columnDefinition = "DECIMAL(4,3)")
    private Double consistencyScore; // Consistency with previous answers

    @Column(name = "cognitive_bias_score", columnDefinition = "DECIMAL(4,3)")
    private Double cognitiveBiasScore; // Overall cognitive bias score

    @Column(name = "time_pressure_score", columnDefinition = "DECIMAL(4,3)")
    private Double timePressureScore; // Score based on time taken vs. average

    @Column(name = "answer_stability", columnDefinition = "DECIMAL(4,3)")
    private Double answerStability; // Stability of answer during editing

    @Column(name = "is_correct", columnDefinition = "BOOLEAN")
    private Boolean isCorrect; // For MCQ questions, whether the answer is correct

    @Column(name = "typing_speed_wpm", columnDefinition = "DECIMAL(5,2)")
    private Double typingSpeedWpm; // Calculated words per minute for text answers

    @Column(name = "similarity_score", columnDefinition = "DECIMAL(4,3)")
    private Double similarityScore; // Cosine similarity to expected answer/keywords for text answers

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = LocalDateTime.now();
    }
}
