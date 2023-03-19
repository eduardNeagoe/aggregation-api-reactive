package com.reactive.api.pricing;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public interface PricingService {

    Mono<Map<String, OptionalDouble>> getPricing(List<String> pricingCountryCodes);
}
