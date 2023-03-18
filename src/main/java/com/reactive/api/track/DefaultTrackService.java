package com.reactive.api.track;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultTrackService implements TrackService {
    private final TrackClient trackClient;

    public Mono<Map<String, Optional<Status>>> getTrack(List<String> trackOrderNumbers) {
        return Flux.fromIterable(trackOrderNumbers)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(trackClient::getTrack)
            .sequential()
            .collectMap(Track::getOrderNumber, Track::getStatus)
            .doOnNext(this::removeEmptyValues);
    }

    private boolean removeEmptyValues(Map<String, Optional<Status>> pricing) {
        return pricing.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

}
