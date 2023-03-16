package com.reactive.api.aggregation;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface AggregationService {
    Mono<Aggregation> aggregate(Optional<List<String>> shipmentsOrderNumbers,
                                Optional<List<String>> trackOrderNumbers,
                                Optional<List<String>> pricingCountryCodes);

}
