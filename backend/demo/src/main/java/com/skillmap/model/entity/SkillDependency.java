package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "skill_dependencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_skill_id", nullable = false)
    @JsonBackReference
    private Skill parentSkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_skill_id", nullable = false)
    @JsonBackReference
    private Skill childSkill;

    @Column(name = "weight", columnDefinition = "DECIMAL(3,2) DEFAULT 1.0")
    private Double weight = 1.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false)
    private DependencyType dependencyType;

    public enum DependencyType {
        prerequisite, complementary, advanced
    }
}
