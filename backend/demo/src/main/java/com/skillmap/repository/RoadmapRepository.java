package com.skillmap.repository;

import com.skillmap.model.entity.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    List<Roadmap> findBySessionId(Long sessionId);

    List<Roadmap> findByUserId(Long userId);
}
