package com.reactive.api.tracking;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class TrackClient {

    private final WebClient pricingWebClient;

    private final ConfigProperties configProperties;


    @Autowired
    public TrackClient(ConfigProperties configProperties) {
        this.pricingWebClient = WebClient.create(configProperties.getTrackBaseUrl());
        this.configProperties = configProperties;
    }

    public Mono<Track> getTrack(String orderNumber) {

        return pricingWebClient.get()
            .uri(configProperties.getTrackUrl(), orderNumber)
            .retrieve()
            .bodyToMono(Status.class)
            .map(status -> new Track(orderNumber, Optional.of(status)))
            .timeout(configProperties.getTrackTimeout(), Mono.just(getFallbackTrack(orderNumber)))
            .onErrorReturn(isServiceUnavailable(), getFallbackTrack(orderNumber))

            // added to handle the WebClientRequestException caused by PrematureCloseException (got this when sent hundreds of request to the pricing service)
            // reactor.netty.http.client.PrematureCloseException: Connection has been closed BEFORE response, while sending request body
            .onErrorReturn(e -> e instanceof WebClientRequestException, getFallbackTrack(orderNumber));
    }

    private Track getFallbackTrack(String orderNumber) {
        return new Track(orderNumber, Optional.empty());
    }

    private Predicate<Throwable> isServiceUnavailable() {
        return e -> e instanceof WebClientResponseException.ServiceUnavailable;
    }
}
