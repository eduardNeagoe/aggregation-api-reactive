package com.reactive.api.shipment;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ShipmentService {
    Mono<Map<String, Optional<List<Product>>>> getShipment(List<String> shipmentsOrderNumbers);
    }
