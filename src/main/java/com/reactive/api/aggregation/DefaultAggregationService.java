package com.reactive.api.aggregation;

import com.reactive.api.config.ConfigProperties;
import com.reactive.api.pricing.PricingService;
import com.reactive.api.shipment.Product;
import com.reactive.api.shipment.ShipmentService;
import com.reactive.api.track.Status;
import com.reactive.api.track.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultAggregationService implements AggregationService {

    public static final Duration ERROR_MARGIN = Duration.ofMillis(200);
    private final ShipmentService shipmentService;
    private final TrackService trackService;
    private final PricingService pricingService;
    private final ConfigProperties configProperties;

    @Override
    public Mono<Aggregation> aggregate(Optional<List<String>> shipmentsOrderNumbers,
                                       Optional<List<String>> trackOrderNumbers,
                                       Optional<List<String>> pricingCountryCodes) {

        log.debug("Processing Aggregation request...");

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
            .timeout(configProperties.getSla().minus(ERROR_MARGIN), Mono.just(new Aggregation()))
            .doOnNext(aggregation -> log.debug("Aggregation finished"));
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
