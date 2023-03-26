package com.reactive.api.shipment;

import com.reactive.api.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "false")
public class DefaultShipmentClient implements ShipmentClient {

    private final WebClient client;

    private final ConfigProperties configProperties;


    @Autowired
    public DefaultShipmentClient(ConfigProperties configProperties) {
        this.client = WebClient.create(configProperties.getShipmentBaseUrl());
        this.configProperties = configProperties;
    }

    public Mono<Shipment> getShipment(String orderNumber) {

        return getShipmentProducts(orderNumber)
            .map(products -> new Shipment(orderNumber, Optional.of(products)))
            .timeout(configProperties.getShipmentProductsTimeout(), getFallbackShipmentMono(orderNumber))

            // handles service unavailable error and other errors
            .onErrorResume(e -> e instanceof WebClientException, e -> getFallbackShipmentMono(orderNumber))
            .doOnNext(shipment -> log.debug("Shipment result: " + shipment));
    }

    private Mono<List<Product>> getShipmentProducts(String orderNumber) {
        return client.get()
            .uri(configProperties.getShipmentProductsUrl(), orderNumber)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });
    }

    private Mono<Shipment> getFallbackShipmentMono(String orderNumber) {
        return Mono.just(getFallbackShipment(orderNumber))
            .doOnNext(track -> log.debug("Falling back on empty shipment"));
    }

    private Shipment getFallbackShipment(String orderNumber) {
        return new Shipment(orderNumber, Optional.empty());
    }
}
