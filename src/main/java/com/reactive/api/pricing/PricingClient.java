package com.reactive.api.pricing;

import reactor.core.publisher.Mono;

public interface PricingClient {

    Mono<Pricing> getPricing(String pricingCountryCode);

}
