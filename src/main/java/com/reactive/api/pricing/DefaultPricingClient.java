package com.reactive.api.pricing;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.OptionalDouble;

@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "false")
public class DefaultPricingClient implements PricingClient {

    private final WebClient client;

    private final ConfigProperties configProperties;


    @Autowired
    public DefaultPricingClient(ConfigProperties configProperties) {
        this.client = WebClient.create(configProperties.getPricingBaseUrl());
        this.configProperties = configProperties;
    }

    public Mono<Pricing> getPricing(String pricingCountryCode) {
        return getPrice(pricingCountryCode)
            .map(price -> new Pricing(pricingCountryCode, OptionalDouble.of(price)))
            .timeout(configProperties.getPricingTimeout(), Mono.just(getFallbackPricing(pricingCountryCode)))

            // handles service unavailable error and other errors
            .onErrorReturn(e -> e instanceof WebClientException, getFallbackPricing(pricingCountryCode));
    }

    private Mono<Double> getPrice(String pricingCountryCode) {
        return client.get()
            .uri(configProperties.getPricingUrl(), pricingCountryCode)
            .retrieve()
            .bodyToMono(Double.class);
    }

    private Pricing getFallbackPricing(String pricingCountryCode) {
        return new Pricing(pricingCountryCode, OptionalDouble.empty());
    }
}
