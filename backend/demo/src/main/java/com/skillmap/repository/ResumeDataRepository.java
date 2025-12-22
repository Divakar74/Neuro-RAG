package com.skillmap.repository;

import com.skillmap.model.entity.ResumeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeDataRepository extends JpaRepository<ResumeData, Long> {

    List<ResumeData> findBySessionId(Long sessionId);

    List<ResumeData> findBySession_User_Id(Long userId);

    List<ResumeData> findByUserId(Long userId);
}
