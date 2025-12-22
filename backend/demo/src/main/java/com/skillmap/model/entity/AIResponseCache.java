package com.skillmap.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_response_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AIResponseCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cache_key", unique = true, nullable = false, length = 255)
    private String cacheKey;
    
    @Column(name = "prompt_hash", nullable = false, length = 64)
    private String promptHash;
    
    @Lob
    @Column(name = "prompt", nullable = false)
    private String prompt;
    
    @Lob
    @Column(name = "response", nullable = false)
    private String response;
    
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType; // e.g., "feedback", "suggestions", "roadmap"
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "model_used", length = 50)
    private String modelUsed;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;
    
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;
    
    // Helper method to check if cache is expired
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    // Helper method to increment hit count
    public void incrementHitCount() {
        this.hitCount = (this.hitCount == null ? 0 : this.hitCount) + 1;
        this.lastAccessed = LocalDateTime.now();
    }
}