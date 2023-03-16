package com.reactive.api.aggregation;

import com.reactive.api.config.ConfigProperties;
import com.reactive.api.pricing.PricingService;
import com.reactive.api.shipment.Product;
import com.reactive.api.shipment.ShipmentService;
import com.reactive.api.tracking.Status;
import com.reactive.api.tracking.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultAggregationService implements AggregationService {

    private final ShipmentService shipmentService;
    private final TrackService trackService;
    private final PricingService pricingService;

    private final ConfigProperties properties;


    @Override
    public Mono<Aggregation> aggregate(Optional<List<String>> shipmentsOrderNumbers,
                                       Optional<List<String>> trackOrderNumbers,
                                       Optional<List<String>> pricingCountryCodes) {


        Mono<Map<String, Optional<List<Product>>>> shipments = shipmentsOrderNumbers
            .map(shipmentService::getShipment)
            .orElse(Mono.just(Map.of()));

        Mono<Map<String, Optional<Status>>> track = trackOrderNumbers
            .map(trackService::getTrack)
            .orElse(Mono.just(Map.of()));

        Mono<Map<String, Optional<Double>>> pricing = pricingCountryCodes
            .map(pricingService::getPricing)
            //TODO OR?
//            .map(pricingService.getPricing().timeout())
//            .timeout(properties.getSla().minus(Duration.ofSeconds(1)))
            .orElse(Mono.just(Map.of()));

        return Mono.zip(shipments, track, pricing)
            //TODO OR?
//            .timeout(properties.getSla().minus(Duration.ofSeconds(1)))
            .map(r -> aggregate(r.getT1(), r.getT2(), r.getT3()));
    }

    private Aggregation aggregate(Map<String, Optional<List<Product>>> shipments,
                                  Map<String, Optional<Status>> track,
                                  Map<String, Optional<Double>> pricing) {

        return Aggregation.builder()
            .shipments(shipments)
            .track(track)
            .pricing(pricing)
            .build();
    }
}
