package com.reactive.api.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultPricingService implements PricingService {
    private final PricingClient pricingClient;

    public Mono<Map<String, Optional<Double>>> getPricing(List<String> pricingCountryCodes) {

        return Flux.fromIterable(pricingCountryCodes)
            .parallel()
            .runOn(Schedulers.parallel())

//            .log()

            .flatMap(pricingClient::getPricing)
            .sequential()

//            .log()

//            .elapsed()
//            .doOnNext(objects -> System.out.println("@@@ Elapsed: " + objects))
//            .map(objects -> objects.getT2())

            .collectMap(Pricing::getCountryCode, Pricing::getPrice)
            .doOnNext(this::removeEmptyValues);

//            .log();
    }

    private boolean removeEmptyValues(Map<String, Optional<Double>> pricing) {
        return pricing.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

}
