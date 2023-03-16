package com.reactive.api.shipment;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class ShipmentClient {

    private final WebClient client;

    private final ConfigProperties configProperties;


    @Autowired
    public ShipmentClient(ConfigProperties configProperties) {
        this.client = WebClient.create(configProperties.getShipmentBaseUrl());
        this.configProperties = configProperties;
    }

    public Mono<Shipment> getShipment(String orderNumber) {

        return client.get()
            .uri(configProperties.getShipmentUrl(), orderNumber)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Product>>() {
            })
            .map(products -> new Shipment(orderNumber, Optional.of(products)))
            .timeout(configProperties.getShipmentTimeout(), Mono.just(getFallbackShipment(orderNumber)))
            .onErrorReturn(isServiceUnavailable(), getFallbackShipment(orderNumber))

            // added to handle the WebClientRequestException caused by PrematureCloseException (got this when sent hundreds of request to the shipment service)
            // reactor.netty.http.client.PrematureCloseException: Connection has been closed BEFORE response, while sending request body
            .onErrorReturn(e -> e instanceof WebClientRequestException, getFallbackShipment(orderNumber));
    }

    private Shipment getFallbackShipment(String orderNumber) {
        return new Shipment(orderNumber, Optional.empty());
    }

    private Predicate<Throwable> isServiceUnavailable() {
        return e -> e instanceof WebClientResponseException.ServiceUnavailable;
    }
}
