package com.reactive.api.aggregation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class AggregationController {

    private final AggregationService aggregationService;

    @GetMapping("/aggregation")
    public Mono<Aggregation> aggregate(@RequestParam Optional<List<String>> shipmentsOrderNumbers,
                                       @RequestParam Optional<List<String>> trackOrderNumbers,
                                       @RequestParam Optional<List<String>> pricingCountryCodes) {
        return aggregationService.aggregate(shipmentsOrderNumbers, trackOrderNumbers, pricingCountryCodes);
    }
}
