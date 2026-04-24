package com.example.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 * 
 * ========================================
 * RULE 16: Redis & Cache Standards
 * ========================================
 * 
 * This configuration implements all 5 sub-rules from ARCHITECTURE-RULES.md:
 * 
 * Rule 16.1: TTL is Mandatory
 *   - All cache regions have explicit TTL configured
 *   - No infinite caches allowed
 *   - TTL values based on data volatility
 * 
 * Rule 16.2: Cache Keys Must Include Tenant ID
 *   - All @Cacheable annotations use tenant-aware keys
 *   - Format: "tenantId:key" or dynamically via TenantContext
 * 
 * Rule 16.3: Cache Eviction on Updates
 *   - All update/delete operations use @CacheEvict
 *   - Related caches cleared together
 * 
 * Rule 16.4: Do Not Cache Pageable Results
 *   - Pagination results NOT cached (too many variations)
 *   - Individual items cached instead
 * 
 * Rule 16.5: Never Cache Security-Sensitive Data
 *   - No passwords, tokens, or sensitive data cached
 *   - Only safe, non-sensitive information
 * 
 * ========================================
 * TTL Categories (based on data volatility):
 * ========================================
 * 
 * 1. Reference Data (24 hours):
 *    - permissions: Permission definitions (rarely change)
 *    - roles: Role definitions (rarely change)
 *    - pages: Page/screen definitions (rarely change)
 * 
 * 2. User Profiles (1 hour):
 *    - userProfiles: User profile information
 *    - userRoles: User-role assignments
 * 
 * 3. Dynamic/Session Data (30 minutes):
 *    - menus: Dynamic menu structures
 *    - userMenus: User-specific menus
 * 
 * 4. Temporary Data (5 minutes):
 *    - tempData: Short-lived helper data
 * 
 * ========================================
 * Activation:
 * ========================================
 * This config is only active when: spring.cache.type=redis
 * - Development: Uses simple cache (application-dev.properties)
 * - Production: Uses Redis cache (application-prod.properties)
 * 
 * @see ARCHITECTURE-RULES.md Rule 16
 */
@Configuration
// @EnableCaching  // ❌ DISABLED: Redis caching disabled - will be enabled later
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig implements CachingConfigurer {

    /**
     * Provide a graceful fallback error handler so that Redis connection
     * failures result in a DB query (cache miss) rather than a 500 error.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new RedisFallbackCacheErrorHandler();
    }

    /**
     * Configure Redis Cache Manager with TTLs for different cache regions
     * 
     * TTL Strategy (Rule 16.1):
     * - Reference data: 24 hours (permissions, roles, pages)
     * - User profiles: 1 hour (userProfiles, userRoles)
     * - Dynamic data: 30 minutes (menus, userMenus)
     * - Temporary data: 5 minutes (tempData)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        // Default cache configuration (5 minutes TTL)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                    )
                )
                .disableCachingNullValues();

        // Specific TTLs for different cache regions (Rule 16.1: TTL is Mandatory)
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // ========================================
        // Category 1: Reference Data (24 hours)
        // ========================================
        // Rarely changes, safe to cache long-term
        cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("roles", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("pages", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // ========================================
        // Category 2: User Profiles (1 hour)
        // ========================================
        // Moderate change frequency
        cacheConfigurations.put("userProfiles", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("userRoles", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // ========================================
        // Category 3: Dynamic Data (30 minutes)
        // ========================================
        // Dynamic menu structures
        cacheConfigurations.put("menus", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("userMenus", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // ========================================
        // Category 4: Temporary Data (5 minutes)
        // ========================================
        // Short-lived helper data
        cacheConfigurations.put("tempData", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
