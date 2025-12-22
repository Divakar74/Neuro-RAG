package com.skillmap.repository;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillAssessmentRepository extends JpaRepository<SkillAssessment, Long> {

    List<SkillAssessment> findBySession(AssessmentSession session);

    Optional<SkillAssessment> findBySessionAndSkill(AssessmentSession session, Skill skill);

    @Query("SELECT sa FROM SkillAssessment sa WHERE sa.session.id = :sessionId")
    List<SkillAssessment> findBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT sa.skill.skillCode, sa.assessedLevel FROM SkillAssessment sa WHERE sa.session.id = :sessionId")
    List<Object[]> findSkillLevelsBySessionId(@Param("sessionId") Long sessionId);

    List<SkillAssessment> findByUserId(Long userId);
}
