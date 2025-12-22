package com.skillmap.model.dto;

import lombok.Data;

@Data
public class ResponseDto {
    private Long sessionId;
    private Long questionId;
    private String responseText;
    private String responseChoice;
    private Integer responseScale;
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
}


