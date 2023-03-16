package com.reactive.api.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
public class Pricing {
    private String countryCode;
    private Optional<Double> price;

}
