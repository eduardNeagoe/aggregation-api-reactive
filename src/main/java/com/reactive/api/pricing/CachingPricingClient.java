package com.reactive.api.pricing;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "true")
public class CachingPricingClient implements PricingClient {

    private final DefaultPricingClient pricingClient;

    private final ReactiveRedisOperations<String, Pricing> operations;

    private final ConfigProperties properties;

    private static final String KEY_PREFIX = "pricing_";

    @Autowired
    public CachingPricingClient(ConfigProperties configProperties,
                                ReactiveRedisOperations<String, Pricing> operations,
                                ConfigProperties properties) {

        this.pricingClient = new DefaultPricingClient(configProperties);
        this.operations = operations;
        this.properties = properties;
    }

    @Override
    public Mono<Pricing> getPricing(String pricingCountryCode) {
        String key = KEY_PREFIX + pricingCountryCode;
        return operations.opsForValue().get(key)
            .onErrorResume(throwable -> Mono.empty())
            .switchIfEmpty(getAndCachePricing(pricingCountryCode, key));
    }

    private Mono<Pricing> getAndCachePricing(String pricingCountryCode, String key) {
        return pricingClient.getPricing(pricingCountryCode)
            .flatMap(pricing -> pricing.getPrice().isEmpty() ? Mono.just(pricing) : cacheThenReturn(key, pricing));
    }

    private Mono<Pricing> cacheThenReturn(String key, Pricing pricing) {
        return operations.opsForValue()
            .set(key, pricing, properties.getExpiration())
            .thenReturn(pricing);
    }
}
