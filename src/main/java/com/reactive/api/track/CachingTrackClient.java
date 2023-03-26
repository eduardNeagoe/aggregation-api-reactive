package com.reactive.api.track;

import com.reactive.api.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "true")
public class CachingTrackClient implements TrackClient {

    private final TrackClient trackClient;

    private final ReactiveRedisOperations<String, Track> operations;

    private final ConfigProperties properties;

    private static final String KEY_PREFIX = "track_";

    @Autowired
    public CachingTrackClient(ConfigProperties configProperties,
                              ReactiveRedisOperations<String, Track> operations,
                              ConfigProperties properties) {

        this.trackClient = new DefaultTrackClient(configProperties);
        this.operations = operations;
        this.properties = properties;
    }

    @Override
    public Mono<Track> getTrack(String trackOrderNumber) {
        String key = KEY_PREFIX + trackOrderNumber;
        return operations.opsForValue().get(key)
            .doOnNext(track -> log.debug("Returning cached track: " + track))
            .onErrorResume(throwable -> Mono.empty())
            .switchIfEmpty(getAndCacheTrack(trackOrderNumber, key));
    }

    private Mono<Track> getAndCacheTrack(String trackOrderNumber, String key) {
        return trackClient.getTrack(trackOrderNumber)
            .flatMap(track -> track.getStatus().isEmpty() ? Mono.just(track) : cacheThenReturn(key, track));
    }

    private Mono<Track> cacheThenReturn(String key, Track track) {
        return operations.opsForValue()
            .set(key, track, properties.getExpiration())
            .thenReturn(track)
            .doOnNext(t -> log.debug("Added to cache - track: " + t));
    }

}
