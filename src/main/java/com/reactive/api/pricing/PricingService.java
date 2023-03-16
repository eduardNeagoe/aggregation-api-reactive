package com.reactive.api.pricing;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PricingService {

    Mono<Map<String, Optional<Double>>> getPricing(List<String> pricingCountryCodes);
}
