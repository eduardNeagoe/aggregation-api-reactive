package com.reactive.api.aggregation;

import com.reactive.api.pricing.PricingService;
import com.reactive.api.shipment.Product;
import com.reactive.api.shipment.ShipmentService;
import com.reactive.api.track.Status;
import com.reactive.api.track.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultAggregationService implements AggregationService {

    private final ShipmentService shipmentService;
    private final TrackService trackService;
    private final PricingService pricingService;

    @Override
    public Mono<Aggregation> aggregate(Optional<List<String>> shipmentsOrderNumbers,
                                       Optional<List<String>> trackOrderNumbers,
                                       Optional<List<String>> pricingCountryCodes) {

        log.info("Aggregation request: {}, {}, {}", shipmentsOrderNumbers, trackOrderNumbers, pricingCountryCodes);

        Mono<Map<String, Optional<List<Product>>>> shipments = shipmentsOrderNumbers
            .map(shipmentService::getShipment)
            .orElse(Mono.just(Map.of()));

        Mono<Map<String, Optional<Status>>> track = trackOrderNumbers
            .map(trackService::getTrack)
            .orElse(Mono.just(Map.of()));

        Mono<Map<String, OptionalDouble>> pricing = pricingCountryCodes
            .map(pricingService::getPricing)
            .orElse(Mono.just(Map.of()));

        return Mono.zip(shipments, track, pricing)
            .map(tuple -> aggregate(tuple.getT1(), tuple.getT2(), tuple.getT3()))
            .doOnNext(aggregation -> log.info("Aggregation result: {}", aggregation));
    }

    private Aggregation aggregate(Map<String, Optional<List<Product>>> shipments,
                                  Map<String, Optional<Status>> track,
                                  Map<String, OptionalDouble> pricing) {

        return Aggregation.builder()
            .shipments(shipments)
            .track(track)
            .pricing(pricing)
            .build();
    }
}
