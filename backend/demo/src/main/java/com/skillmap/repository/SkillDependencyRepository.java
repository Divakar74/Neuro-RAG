package com.skillmap.repository;

import com.skillmap.model.entity.SkillDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillDependencyRepository extends JpaRepository<SkillDependency, Long> {

    List<SkillDependency> findByParentSkillId(Long parentSkillId);

    List<SkillDependency> findByChildSkillId(Long childSkillId);
}
