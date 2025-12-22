package com.skillmap.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResumeParseDTO {
    private List<ExtractedSkill> extractedSkills;
    private List<String> education;
    private int totalYearsExperience;
}
