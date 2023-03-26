package com.reactive.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties("aggregation")
@Component
@Getter
@Setter
public class ConfigProperties {
    private String baseUrl;
    private String url;
    private Duration sla;

    private String shipmentBaseUrl;
    private String shipmentProductsUrl;
    private Duration shipmentProductsTimeout;


    private String trackBaseUrl;
    private String trackStatusUrl;
    private Duration trackStatusTimeout;


    private String pricingBaseUrl;
    private String pricingUrl;
    private Duration pricingTimeout;

    private Duration apisTimeout;

    private Cache cache;

    public String getCacheHost() {
        return cache.getHost();
    }

    public String getCachePort() {
        return cache.getPort();
    }

    public Duration getExpiration() {
        return cache.getExpiration();
    }

    @Getter
    @Setter
    static class Cache {
        private boolean enabled;
        private String host;
        private String port;
        private Duration expiration;
    }

}
