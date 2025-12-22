package com.skillmap.repository;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.SessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    Optional<SessionFeedback> findBySession(AssessmentSession session);

    Optional<SessionFeedback> findBySessionId(Long sessionId);

    Optional<SessionFeedback> findByUserId(Long userId);
}
