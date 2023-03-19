package com.reactive.api.track;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "true")
public class CachingTrackClient extends DefaultTrackClient {

    private final ReactiveRedisOperations<String, Track> operations;

    private final ConfigProperties properties;

    private static final String KEY_PREFIX = "track_";

    @Autowired
    public CachingTrackClient(ConfigProperties configProperties,
                              ReactiveRedisOperations<String, Track> operations,
                              ConfigProperties properties) {

        super(configProperties);
        this.operations = operations;
        this.properties = properties;
    }

    @Override
    public Mono<Track> getTrack(String trackOrderNumber) {
        String key = KEY_PREFIX + trackOrderNumber;
        return operations.opsForValue().get(key)
            .onErrorResume(throwable -> Mono.empty())
            .switchIfEmpty(getAndCacheTrack(trackOrderNumber, key));
    }

    private Mono<Track> getAndCacheTrack(String trackOrderNumber, String key) {
        return super.getTrack(trackOrderNumber)
            .flatMap(track -> track.getStatus().isEmpty() ? Mono.just(track) : cacheThenReturn(key, track));
    }

    private Mono<Track> cacheThenReturn(String key, Track track) {
        return operations.opsForValue()
            .set(key, track, properties.getExpiration())
            .thenReturn(track);
    }

}
