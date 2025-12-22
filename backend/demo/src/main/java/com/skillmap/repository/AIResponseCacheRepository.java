package com.skillmap.repository;

import com.skillmap.model.entity.AIResponseCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AIResponseCacheRepository extends JpaRepository<AIResponseCache, Long> {
    
    /**
     * Find cached response by cache key
     */
    Optional<AIResponseCache> findByCacheKey(String cacheKey);
    
    /**
     * Find cached response by prompt hash and request type
     */
    Optional<AIResponseCache> findByPromptHashAndRequestType(String promptHash, String requestType);

    /**
     * Find cached response by user ID, prompt hash and request type
     */
    Optional<AIResponseCache> findByUserIdAndPromptHashAndRequestType(Long userId, String promptHash, String requestType);
    
    /**
     * Find all cached responses for a session
     */
    List<AIResponseCache> findBySessionId(Long sessionId);
    
    /**
     * Find all cached responses by request type
     */
    List<AIResponseCache> findByRequestType(String requestType);
    
    /**
     * Find all expired cache entries
     */
    @Query("SELECT c FROM AIResponseCache c WHERE c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    List<AIResponseCache> findExpiredEntries(@Param("now") LocalDateTime now);
    
    /**
     * Delete all expired cache entries
     */
    @Modifying
    @Query("DELETE FROM AIResponseCache c WHERE c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    void deleteExpiredEntries(@Param("now") LocalDateTime now);
    
    /**
     * Find least recently used cache entries
     */
    @Query("SELECT c FROM AIResponseCache c ORDER BY c.lastAccessed ASC, c.hitCount ASC")
    List<AIResponseCache> findLeastRecentlyUsed();
    
    /**
     * Get cache statistics
     */
    @Query("SELECT COUNT(c), SUM(c.hitCount), AVG(c.hitCount) FROM AIResponseCache c")
    Object[] getCacheStatistics();
    
    /**
     * Count cache entries by request type
     */
    Long countByRequestType(String requestType);
    
    /**
     * Delete old cache entries beyond a certain count (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM AIResponseCache c WHERE c.id IN " +
           "(SELECT c2.id FROM AIResponseCache c2 ORDER BY c2.lastAccessed ASC, c2.hitCount ASC " +
           "LIMIT :excessCount)")
    void deleteOldestEntries(@Param("excessCount") int excessCount);
}