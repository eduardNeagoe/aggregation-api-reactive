package com.reactive.api.shipment;

import reactor.core.publisher.Mono;

public interface ShipmentClient {
    Mono<Shipment> getShipment(String orderNumber);
}
