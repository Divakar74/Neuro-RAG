package com.skillmap.repository;

import com.skillmap.model.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE q.skill.id = :skillId AND q.difficultyLevel BETWEEN :minDifficulty AND :maxDifficulty ORDER BY ABS(q.difficultyLevel - :targetDifficulty) ASC")
    List<Question> findBestMatchingQuestion(
        @Param("skillId") Long skillId,
        @Param("targetDifficulty") Double targetDifficulty,
        @Param("minDifficulty") Double minDifficulty,
        @Param("maxDifficulty") Double maxDifficulty
    );

    List<Question> findBySkillId(Long skillId);

    Optional<Question> findByQuestionText(String questionText);
}
