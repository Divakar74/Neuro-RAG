package com.skillmap.repository;

import com.skillmap.model.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {

    List<Response> findBySessionId(Long sessionId);

    List<Response> findByUserId(Long userId);

    @Query("select r.question.id from Response r where r.session.id = :sessionId")
    List<Long> findAnsweredQuestionIds(@Param("sessionId") Long sessionId);
}
