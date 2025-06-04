package com.reactive.api.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor
public class DefaultPricingService implements PricingService {
    private final PricingClient pricingClient;

    public Mono<Map<String, OptionalDouble>> getPricing(List<String> pricingCountryCodes) {
        return Flux.fromIterable(pricingCountryCodes)
            .flatMap(pricingClient::getPricing)
            .collectMap(Pricing::getCountryCode, Pricing::getPrice)
            .doOnNext(this::removeEmptyValues);
    }

    private boolean removeEmptyValues(Map<String, OptionalDouble> pricing) {
        return pricing.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
