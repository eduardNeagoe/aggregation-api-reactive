package com.reactive.api.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.OptionalDouble;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pricing {
    private String countryCode;
    private OptionalDouble price;

}
