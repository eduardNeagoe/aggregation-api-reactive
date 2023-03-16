package com.reactive.api.track;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TrackService {
    Mono<Map<String, Optional<Status>>> getTrack(List<String> pricingCountryCodes);

}
