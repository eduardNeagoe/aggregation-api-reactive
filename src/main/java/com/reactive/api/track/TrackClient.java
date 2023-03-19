package com.reactive.api.track;

import reactor.core.publisher.Mono;

public interface TrackClient {

    Mono<Track> getTrack(String orderNumber);

}
