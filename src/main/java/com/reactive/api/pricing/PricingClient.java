package com.reactive.api.pricing;

import com.reactive.api.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class PricingClient {

    private final WebClient pricingWebClient;

    private final ConfigProperties configProperties;


    @Autowired
    public PricingClient(ConfigProperties configProperties) {
        this.pricingWebClient = WebClient.create(configProperties.getPricingBaseUrl());
        this.configProperties = configProperties;
    }

    public Mono<Pricing> getPricing(String pricingCountryCode) {

        return pricingWebClient.get()
            .uri(configProperties.getPricingUrl(), pricingCountryCode)
            .retrieve()
            .bodyToMono(Double.class)
            .map(price -> new Pricing(pricingCountryCode, Optional.of(price)))
            .timeout(configProperties.getPricingTimeout(), Mono.just(getFallbackPricing(pricingCountryCode)))
            .onErrorReturn(isServiceUnavailable(), getFallbackPricing(pricingCountryCode))

            // added to handle the WebClientRequestException caused by PrematureCloseException (got this when sent hundreds of request to the pricing service)
            // reactor.netty.http.client.PrematureCloseException: Connection has been closed BEFORE response, while sending request body
            .onErrorReturn(e -> e instanceof WebClientRequestException, getFallbackPricing(pricingCountryCode));
    }

    private Pricing getFallbackPricing(String pricingCountryCode) {
        return new Pricing(pricingCountryCode, Optional.empty());
    }

    private Predicate<Throwable> isServiceUnavailable() {
        return e -> e instanceof WebClientResponseException.ServiceUnavailable;
    }
}
