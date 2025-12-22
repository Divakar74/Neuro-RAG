package com.skillmap.repository;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    Optional<AssessmentSession> findBySessionToken(String sessionToken);
    List<AssessmentSession> findByUserId(Long userId);
    List<AssessmentSession> findByUser(User user);
}
