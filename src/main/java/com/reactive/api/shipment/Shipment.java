package com.reactive.api.shipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
public class Shipment {
    private String orderNumber;
    private Optional<List<Product>> products;

}
