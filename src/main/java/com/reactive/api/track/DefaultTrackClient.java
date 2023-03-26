package com.reactive.api.track;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.Optional;

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
            .timeout(configProperties.getTrackStatusTimeout(), Mono.just(getFallbackTrack(orderNumber)))

            // handles service unavailable error and other errors
            .onErrorReturn(e -> e instanceof WebClientException, getFallbackTrack(orderNumber));
    }

    private Mono<Status> getTrackStatus(String orderNumber) {
        return client.get()
            .uri(configProperties.getTrackStatusUrl(), orderNumber)
            .retrieve()
            .bodyToMono(Status.class);
    }

    private Track getFallbackTrack(String orderNumber) {
        return new Track(orderNumber, Optional.empty());
    }

}
