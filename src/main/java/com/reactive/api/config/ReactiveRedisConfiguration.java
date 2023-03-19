package com.reactive.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactive.api.pricing.Pricing;
import com.reactive.api.shipment.Shipment;
import com.reactive.api.track.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Objects;

@Configuration
public class ReactiveRedisConfiguration {


    private final ConfigProperties properties;

    @Autowired
    public ReactiveRedisConfiguration(ConfigProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory(
            Objects.requireNonNull(properties.getCacheHost()),
            Integer.parseInt(Objects.requireNonNull(properties.getCachePort()))
        );
    }

    @Bean
    public ReactiveRedisOperations<String, Pricing> reactiveRedisOperationsPricing(ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        Jackson2JsonRedisSerializer<Pricing> serializer = new Jackson2JsonRedisSerializer<>(mapper, Pricing.class);

        RedisSerializationContext<String, Pricing> context =
            RedisSerializationContext.<String, Pricing>newSerializationContext(new StringRedisSerializer())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisOperations<String, Shipment> reactiveRedisOperationsShipments(ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        Jackson2JsonRedisSerializer<Shipment> serializer = new Jackson2JsonRedisSerializer<>(mapper, Shipment.class);

        RedisSerializationContext<String, Shipment> context =
            RedisSerializationContext.<String, Shipment>newSerializationContext(new StringRedisSerializer())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisOperations<String, Track> reactiveRedisOperationsTrack(ReactiveRedisConnectionFactory factory, ObjectMapper mapper) {
        Jackson2JsonRedisSerializer<Track> serializer = new Jackson2JsonRedisSerializer<>(mapper, Track.class);

        RedisSerializationContext<String, Track> context =
            RedisSerializationContext.<String, Track>newSerializationContext(new StringRedisSerializer())
                .value(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}

