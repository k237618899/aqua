package icu.samnyan.aqua.spring.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Enables Spring's declarative caching ({@code @Cacheable} / {@code @CacheEvict})
 * and provides a bounded Caffeine-backed {@link CacheManager}.
 *
 * The cache must be bounded: pv_list content is large (900+ songs) and the
 * diva server runs on SQLite by default, so an unbounded cache would slowly
 * grow memory. A single-entry cache for divaPvList is enough; the size limit
 * mainly protects other caches (e.g. chusan/music) that already use @Cacheable.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats());
        return manager;
    }
}
