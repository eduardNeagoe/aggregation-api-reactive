package com.reactive.api;


import com.reactive.api.aggregation.Aggregation;
import com.reactive.api.config.ConfigProperties;
import com.reactive.api.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT2M")
class SLATest {
    private double aggregationSLA;

    private Map<Integer, Double> requestDurationMonitor = new ConcurrentHashMap<>();

    private String aggregationUrl;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConfigProperties properties;

    @BeforeEach
    public void prepare() {
        // the SLA of the aggregation API in millis
        aggregationSLA = Long.valueOf(properties.getSla().toMillis()).doubleValue();
        requestDurationMonitor.clear();
        aggregationUrl = properties.getBaseUrl() + properties.getUrl();
    }

    @Test
    @DisplayName("Aggregation should respect the SLA for the 99th percentile")
    void aggregationSLA() {

        int numberOfRequests = 100;

        String countryCodes = TestUtil.getAllCountryCodes(); // 249 codes
        String orderNumbers = TestUtil.generateOrderNumbers(300);

        IntStream.rangeClosed(1, numberOfRequests)
            // number of threads = number of CPU cores - 1
            .parallel()
            .forEach(requestNumber -> {

                StopWatch stopWatch = startNewStopWatch();

                Aggregation aggregation = webTestClient.get()
                    .uri(aggregationUrl, orderNumbers, orderNumbers, countryCodes)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Aggregation.class)
                    .returnResult()
                    .getResponseBody();

                double requestDuration = getDurationInMilliSeconds(stopWatch);
                requestDurationMonitor.put(requestNumber, requestDuration);

                assertNotNull(aggregation);
            });

        Double durationFor99thPercentile = getDurationFor99thPercentile(requestDurationMonitor, numberOfRequests);

        System.out.println("\n⏱️ Durations" +  requestDurationMonitor.values().stream().sorted().toList());
        System.out.println("\n⏱️ 99th percentile " +  durationFor99thPercentile);


        //assert that 99% of the total number of requests take less than 5s
        assertTrue(durationFor99thPercentile < aggregationSLA,
            "99th percentile duration was: %s".formatted(durationFor99thPercentile));
    }

    private Double getDurationFor99thPercentile(Map<Integer, Double> requestDurationMonitor, double numberOfRequests) {
        List<Double> sortedDurations = requestDurationMonitor.values().stream().sorted().toList();

        int ninetyNinePercentOfNumberOfReq = (int) Math.round(numberOfRequests * 0.99);
        int indexOf99thPercentile = ninetyNinePercentOfNumberOfReq - 1;

        return sortedDurations.get(indexOf99thPercentile);
    }

    private StopWatch startNewStopWatch() {
        StopWatch batchStopWatch = new StopWatch();
        batchStopWatch.start();
        return batchStopWatch;
    }

    private double getDurationInMilliSeconds(StopWatch requestStopWatch) {
        requestStopWatch.stop();
        return requestStopWatch.getTotalTimeMillis();
    }
}
