package com.reactive.api.shipment;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

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
            .timeout(configProperties.getShipmentProductsTimeout(), Mono.just(getFallbackShipment(orderNumber)))

            // handles service unavailable error and other errors
            .onErrorReturn(e -> e instanceof WebClientException, getFallbackShipment(orderNumber));
    }

    private Mono<List<Product>> getShipmentProducts(String orderNumber) {
        return client.get()
            .uri(configProperties.getShipmentProductsUrl(), orderNumber)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {
            });
    }

    private Shipment getFallbackShipment(String orderNumber) {
        return new Shipment(orderNumber, Optional.empty());
    }
}
