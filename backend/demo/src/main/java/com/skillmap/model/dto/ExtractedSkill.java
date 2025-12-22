package com.skillmap.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExtractedSkill {
    private String skillCode;
    private String skillName;
    private int yearsExperience;
    private double confidence;
}
