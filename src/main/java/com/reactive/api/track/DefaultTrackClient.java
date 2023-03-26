package com.reactive.api.track;

import com.reactive.api.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "false")
public class DefaultTrackClient implements TrackClient {

    private final WebClient client;

    private final ConfigProperties configProperties;


    @Autowired
    public DefaultTrackClient(ConfigProperties configProperties) {
        this.client = WebClient.create(configProperties.getTrackBaseUrl());
        this.configProperties = configProperties;
    }

    public Mono<Track> getTrack(String orderNumber) {

        return getTrackStatus(orderNumber)
            .map(status -> new Track(orderNumber, Optional.of(status)))
            .timeout(configProperties.getTrackStatusTimeout(), getFallbackTrackMono(orderNumber))

            // handles service unavailable error and other errors
            .onErrorResume(e -> e instanceof WebClientException, e -> getFallbackTrackMono(orderNumber))
            .doOnNext(track -> log.debug("Track result: " + track));
    }

    private Mono<Status> getTrackStatus(String orderNumber) {
        return client.get()
            .uri(configProperties.getTrackStatusUrl(), orderNumber)
            .retrieve()
            .bodyToMono(Status.class);
    }

    private Mono<Track> getFallbackTrackMono(String orderNumber) {
        return Mono.just(getFallbackTrack(orderNumber))
            .doOnNext(track -> log.debug("Falling back on empty track"));
    }

    private Track getFallbackTrack(String orderNumber) {
        return new Track(orderNumber, Optional.empty());
    }

}
