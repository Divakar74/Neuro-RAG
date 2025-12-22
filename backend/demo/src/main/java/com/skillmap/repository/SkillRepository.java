package com.skillmap.repository;

import com.skillmap.model.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findBySkillCode(String skillCode);

    Optional<Skill> findByDisplayName(String displayName);

    List<Skill> findByCategory(Skill.Category category);

    @Query("SELECT s FROM Skill s WHERE s.importanceWeight > :weight")
    List<Skill> findByImportanceWeightGreaterThan(@Param("weight") Double weight);

    @Query(value = "SELECT * FROM skills s WHERE JSON_CONTAINS(s.keywords, :keyword)", nativeQuery = true)
    List<Skill> findByKeyword(@Param("keyword") String keyword);
}
