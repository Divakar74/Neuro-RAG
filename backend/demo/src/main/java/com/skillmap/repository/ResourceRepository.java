package com.skillmap.repository;

import com.skillmap.model.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findBySkillId(Long skillId);

    List<Resource> findBySkillIdAndTargetLevel(Long skillId, Integer targetLevel);

    @Query("SELECT r FROM Resource r WHERE r.skill.id = :skillId AND r.targetLevel <= :targetLevel ORDER BY r.rating DESC, r.estimatedHours ASC")
    List<Resource> findRecommendedResources(@Param("skillId") Long skillId, @Param("targetLevel") Integer targetLevel);
}
