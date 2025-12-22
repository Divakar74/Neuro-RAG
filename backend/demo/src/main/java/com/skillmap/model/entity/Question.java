package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    @JsonBackReference
    private Skill skill;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "question_type", nullable = false)
    private String questionType = "text"; // "text" for open-ended, "mcq" for multiple choice

    @Column(name = "difficulty_level", columnDefinition = "DECIMAL(2,1)")
    private Double difficultyLevel; // For backward compatibility, but will use difficulty for MCQ

    @Column(name = "difficulty", nullable = false)
    private String difficulty; // "Easy", "Intermediate", "Advanced" for MCQ, or mapped for text

    @Column(name = "options", columnDefinition = "JSON")
    private String options; // JSON array of options for MCQ

    @Column(name = "correct_answer")
    private String correctAnswer; // Correct answer for MCQ

    // Helper method to get options as List
    public List<String> getOptionsList() {
        if (options == null || options.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                options,
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    @Column(name = "topic")
    private String topic; // Topic/category for MCQ

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation; // Explanation for MCQ

    @Column(name = "expected_keywords", columnDefinition = "JSON")
    private String expectedKeywords;

    @Column(name = "level_indicators", columnDefinition = "JSON")
    private String levelIndicators;

    @Column(name = "suggested_answer_length")
    private Integer suggestedAnswerLength;

    @Column(name = "context_hint", columnDefinition = "TEXT")
    private String contextHint;

    @Column(name = "follow_up_text", columnDefinition = "TEXT")
    private String followUpText;

    @Column(name = "times_asked")
    private Integer timesAsked;

    @Column(name = "avg_response_time")
    private Integer avgResponseTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
