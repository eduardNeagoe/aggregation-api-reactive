package com.reactive.api.shipment;

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
public class CachingShipmentClient implements ShipmentClient {

    private final ShipmentClient shipmentClient;

    private final ReactiveRedisOperations<String, Shipment> operations;

    private final ConfigProperties properties;

    private static final String KEY_PREFIX = "shipment_";

    @Autowired
    public CachingShipmentClient(ConfigProperties configProperties,
                                 ReactiveRedisOperations<String, Shipment> operations,
                                 ConfigProperties properties) {

        this.shipmentClient = new DefaultShipmentClient(configProperties);
        this.operations = operations;
        this.properties = properties;
    }

    @Override
    public Mono<Shipment> getShipment(String shipmentsOrderNumbers) {
        String key = KEY_PREFIX + shipmentsOrderNumbers;
        return operations.opsForValue().get(key)
            .doOnNext(shipment -> log.debug("Returning cached shipment: " + shipment))
            .onErrorResume(throwable -> Mono.empty())
            .switchIfEmpty(getAndCacheShipment(shipmentsOrderNumbers, key));
    }

    private Mono<Shipment> getAndCacheShipment(String shipmentsOrderNumbers, String key) {
        return shipmentClient.getShipment(shipmentsOrderNumbers)
            .flatMap(shipment -> shipment.getProducts().isEmpty() ? Mono.just(shipment) : cacheThenReturn(key, shipment));

    }

    private Mono<Shipment> cacheThenReturn(String key, Shipment shipment) {
        return operations.opsForValue()
            .set(key, shipment, properties.getExpiration())
            .thenReturn(shipment)
            .doOnNext(s -> log.debug("Added to cache - shipment: " + s));
    }

}
