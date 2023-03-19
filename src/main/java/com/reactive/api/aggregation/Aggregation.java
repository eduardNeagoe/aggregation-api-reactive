package com.reactive.api.aggregation;

import com.reactive.api.shipment.Product;
import com.reactive.api.track.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Aggregation {

    private Map<String, Optional<List<Product>>> shipments = new HashMap<>();
    private Map<String, Optional<Status>> track = new HashMap<>();
    private Map<String, OptionalDouble> pricing = new HashMap<>();
}
