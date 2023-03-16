package com.reactive.api.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
public class Track {
    private String orderNumber;
    private Optional<Status> status;

}
