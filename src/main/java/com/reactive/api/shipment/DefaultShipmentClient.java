package com.reactive.api.shipment;

import com.reactive.api.config.ConfigProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@ConditionalOnProperty(name = "aggregation.cache.enabled", havingValue = "false")
public class DefaultShipmentClient implements ShipmentClient {

    private final WebClient client;

    private final ConfigProperties configProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private  io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

//
//    //    TODO add circuit breaker to avoid waiting for the timeout threshold for each request when the bakend api is down
//    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = io.github.resilience4j.circuitbreaker.CircuitBreaker.of("aggregation",
//        CircuitBreakerConfig.custom()
//            .failureRateThreshold(2)
//            .recordExceptions(
//                WebClientResponseException.class,
//                WebClientResponseException.InternalServerError.class
//            )
//            .slidingWindowSize(5)
//            .waitDurationInOpenState(Duration.ofMillis(1000))
//            .permittedNumberOfCallsInHalfOpenState(2)
//            .build()
//    );

    @Autowired
    public DefaultShipmentClient(ConfigProperties configProperties) {
        this.client = WebClient.create(configProperties.getShipmentBaseUrl());
        this.configProperties = configProperties;
    }

    //    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "aggregation-cb", fallbackMethod = "getFallbackShipment")
//    @CircuitBreaker(name = "aggregation", fallbackMethod = "getFallbackShipment")
    public Mono<Shipment> getShipment(String orderNumber) {
//        ReactorFallbackDecorator reactorFallbackDecorator = new ReactorFallbackDecorator();
//
//        CheckedSupplier<Object[]> supplier = () -> new Mono[]{getProducts(orderNumber)};
//        reactorFallbackDecorator.decorate(FallbackMethod.create("getFallbackShipment", Objects.requireNonNull(ReflectionUtils.findMethod(getClass(), "getProducts")), supplier ))

        return getProducts(orderNumber)
            .map(products -> new Shipment(orderNumber, Optional.of(products)));
//            .switchIfEmpty(Mono.just(getFallbackShipment(orderNumber)));
//            .timeout(configProperties.getShipmentTimeout(), Mono.just(getFallbackShipment(orderNumber)))

//            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))


//            .onErrorReturn(e -> e instanceof CallNotPermittedException, getFallbackShipment(orderNumber))


//            .onErrorReturn(isServiceUnavailable(), getFallbackShipment(orderNumber))

            // added to handle the WebClientRequestException caused by PrematureCloseException (got this when sent hundreds of request to the shipment service)
            // reactor.netty.http.client.PrematureCloseException: Connection has been closed BEFORE response, while sending request body
//            .onErrorReturn(e -> e instanceof WebClientRequestException, getFallbackShipment(orderNumber));
    }

    @CircuitBreaker(name = "aggregation", fallbackMethod = "getFallbackProducts")
    private Mono<List<Product>> getProducts(String orderNumber){
//        io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateFunction(circuitBreaker, on -> Mono.error(new RuntimeException()))

//        circuitBreaker.
        return client.get()
            .uri(configProperties.getShipmentUrl(), orderNumber)
            .retrieve()

            .bodyToMono(new ParameterizedTypeReference<>() {
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .map(o -> List.of());
//        return Mono.error(new RuntimeException());
    }

    private Mono<List<Product>> getFallbackProducts(String orderNumber, Exception e) {
        System.out.println("⭕️ fallback");
        return Mono.just(List.of());
    }

    private Shipment getFallbackShipment(String orderNumber) {
        return new Shipment(orderNumber, Optional.empty());
    }

    private Predicate<Throwable> isServiceUnavailable() {
        return e -> e instanceof WebClientResponseException.ServiceUnavailable;
    }
}
