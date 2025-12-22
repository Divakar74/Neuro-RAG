package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_code", unique = true, nullable = false)
    private String skillCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "importance_weight", columnDefinition = "DECIMAL(3,2) DEFAULT 1.0")
    private Double importanceWeight = 1.0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "level_descriptors", columnDefinition = "JSON")
    private String levelDescriptors;

    @Column(columnDefinition = "JSON")
    private String keywords;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Question> questions;

    @OneToMany(mappedBy = "parentSkill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SkillDependency> parentDependencies;

    @OneToMany(mappedBy = "childSkill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SkillDependency> childDependencies;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SkillAssessment> skillAssessments;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Resource> resources;

    public enum Category {
        Programming, Systems, Data, Cloud, Soft_Skills
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
