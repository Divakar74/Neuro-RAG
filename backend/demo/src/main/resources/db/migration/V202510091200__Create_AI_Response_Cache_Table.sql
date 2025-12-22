-- Create AI Response Cache table for storing OpenAI responses to avoid token wastage
CREATE TABLE ai_response_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(255) UNIQUE NOT NULL,
    prompt_hash VARCHAR(64) NOT NULL,
    prompt LONGTEXT NOT NULL,
    response LONGTEXT NOT NULL,
    session_id BIGINT,
    request_type VARCHAR(50) NOT NULL,
    tokens_used INT,
    model_used VARCHAR(50),
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    hit_count INT NOT NULL DEFAULT 0,
    last_accessed TIMESTAMP NULL,
    
    -- Indexes for performance
    INDEX idx_cache_key (cache_key),
    INDEX idx_prompt_hash_type (prompt_hash, request_type),
    INDEX idx_session_id (session_id),
    INDEX idx_request_type (request_type),
    INDEX idx_expires_at (expires_at),
    INDEX idx_last_accessed (last_accessed),
    
    -- Foreign key constraint
    CONSTRAINT fk_ai_cache_session FOREIGN KEY (session_id) REFERENCES assessment_sessions(id) ON DELETE CASCADE
);

-- Add some useful comments
ALTER TABLE ai_response_cache COMMENT = 'Cache table for AI/OpenAI responses to minimize token usage and improve performance';

-- Create a cleanup event to automatically remove expired entries (optional)
-- This can be done via scheduled tasks instead if preferred
-- DELIMITER ;;
-- CREATE EVENT cleanup_expired_ai_cache
-- ON SCHEDULE EVERY 1 HOUR
-- STARTS CURRENT_TIMESTAMP
-- DO
-- BEGIN
--   DELETE FROM ai_response_cache WHERE expires_at IS NOT NULL AND expires_at < NOW();
-- END;;
-- DELIMITER ;