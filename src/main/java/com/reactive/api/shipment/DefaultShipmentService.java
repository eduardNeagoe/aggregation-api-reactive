package com.reactive.api.shipment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultShipmentService implements ShipmentService {
    private final ShipmentClient shipmentClient;

    public Mono<Map<String, Optional<List<Product>>>> getShipment(List<String> shipmentsOrderNumbers) {

        return Flux.fromIterable(shipmentsOrderNumbers)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(shipmentClient::getShipment)
            .sequential()
            .collectMap(Shipment::getOrderNumber, Shipment::getProducts)
            .doOnNext(this::removeEmptyValues);
    }

    private boolean removeEmptyValues(Map<String, Optional<List<Product>>> pricingMap) {
        return pricingMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

}
