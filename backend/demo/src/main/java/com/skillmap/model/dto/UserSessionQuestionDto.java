package com.skillmap.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionQuestionDto {
    // Session information
    private Long sessionId;
    private String sessionToken;
    private LocalDateTime sessionStartedAt;
    private String targetRole;

    // Question information
    private Long questionId;
    private String questionText;
    private String questionType;
    private String difficulty;
    private String topic;
    private String options;
    private String correctAnswer;
    private String explanation;

    // Response information (null if not answered)
    private Long responseId;
    private String responseText;
    private Integer wordCount;
    private Integer charCount;
    private Integer totalTimeSeconds;
    private Integer thinkTimeSeconds;
    private Integer editCount;
    private Boolean pasteDetected;
    private Double confidenceLevel;
    private Boolean isCorrect;
    private Double typingSpeedWpm;
    private Double similarityScore;
    private Double specificityScore;
    private Double depthScore;
    private LocalDateTime answeredAt;
}
