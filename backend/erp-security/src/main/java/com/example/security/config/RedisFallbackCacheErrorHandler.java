package com.example.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Redis Cache Fallback Error Handler
 *
 * <p>When Redis is unavailable, this handler suppresses the connection failure
 * and allows the application to fall back to the DB. Without this handler,
 * any Redis connection error would propagate as a 500 Internal Server Error.
 *
 * <p>Behaviour per operation:
 * <ul>
 *   <li>GET  — logs WARN, returns null (treated as cache miss → method executes)</li>
 *   <li>PUT  — logs WARN, no-op (method result already returned to caller)</li>
 *   <li>EVICT — logs WARN, no-op</li>
 *   <li>CLEAR — logs WARN, no-op</li>
 * </ul>
 *
 * @author ERP Team
 */
@Slf4j
public class RedisFallbackCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[CACHE] Redis GET failed for cache='{}', key='{}'. Falling back to DB. Error: {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("[CACHE] Redis PUT failed for cache='{}', key='{}'. Error: {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[CACHE] Redis EVICT failed for cache='{}', key='{}'. Error: {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("[CACHE] Redis CLEAR failed for cache='{}'. Error: {}",
                cache.getName(), exception.getMessage());
    }
}
