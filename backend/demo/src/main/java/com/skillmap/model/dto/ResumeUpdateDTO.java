package com.skillmap.model.dto;

import lombok.Data;

@Data
public class ResumeUpdateDTO {
    private String extractedSkills;
    private String extractedEducation;
    private String extractedExperience;
    private Integer totalYearsExperience;
}
