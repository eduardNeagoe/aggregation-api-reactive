package com.reactive.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.reactive.api.aggregation.Aggregation;
import com.reactive.api.config.ConfigProperties;
import com.reactive.api.config.JacksonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT2M")
@Testcontainers
class SLATest {
    private double aggregationSLA;

    private HashMap<Integer, Double> requestDurationMonitor = new HashMap<>();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConfigProperties properties;

    //    @LocalServerPort
//    private String serverPort;
    private String uri;
    public static final String BACKEND_SERVICES_CONTAINER = "qwkz/backend-services:latest";
    @Container
    public GenericContainer<?> backedServicesContainer = new GenericContainer(DockerImageName.parse(BACKEND_SERVICES_CONTAINER))
        .withExposedPorts(4000);

    @BeforeEach
    public void prepare() {
        // the SLA of the aggregation API in millis
        aggregationSLA = Long.valueOf(properties.getSla().toMillis()).doubleValue();
        requestDurationMonitor.clear();
        org.testcontainers.Testcontainers.exposeHostPorts(4000);
        uri = backedServicesContainer.getHost() + ":" + backedServicesContainer.getFirstMappedPort() + "/" + properties.getUrl();

    }

    @Test
    @DisplayName("Aggregation should respect the SLA for the 99th percentile")
    void aggregationSLA() {
        // TODO set to 1000
        int numberOfRequests = 10;

        String countryCodes = TestUtil.getAllCountryCodes(); // 249 codes
        String orderNumbers = generateOrderNumbers(300);

        IntStream.rangeClosed(1, numberOfRequests)
            .parallel()
            .forEach(requestNumber -> {

                StopWatch stopWatch = startNewStopWatch();

                Aggregation aggregation = webTestClient.get()
                    .uri(uri, orderNumbers, orderNumbers, countryCodes)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Aggregation.class)
                    .returnResult()
                    .getResponseBody();

                double requestDuration = getDurationInMilliSeconds(stopWatch);
                requestDurationMonitor.put(requestNumber, requestDuration);

                //TODO remove
                printRequestAggregationResult(requestNumber, aggregation);

                assertNotNull(aggregation);
            });

        Double durationFor99thPercentile = getDurationFor99thPercentile(requestDurationMonitor, numberOfRequests);

        //assert that 99% of the total number of requests take less than 5s
        assertTrue(durationFor99thPercentile < aggregationSLA,
            "99th percentile duration was: %s".formatted(durationFor99thPercentile));
    }

    private String getAggregationUrl() {
//        return properties.getBaseUrl() + properties.getUrl();
        return backedServicesContainer.getHost() + ":" + backedServicesContainer.getFirstMappedPort() + "/" + properties.getUrl();
    }

    private Double getDurationFor99thPercentile(HashMap<Integer, Double> requestDurationMonitor, double numberOfRequests) {
        List<Double> sortedDurations = requestDurationMonitor.values().stream().sorted().toList();
        //TODO remove
        System.out.println("⏱️ Durations sorted ASC: " + sortedDurations);

        int ninetyNinePercentOfNumberOfReq = (int) Math.round(numberOfRequests * 0.99);
        int indexOf99thPercentile = ninetyNinePercentOfNumberOfReq - 1;

        return sortedDurations.get(indexOf99thPercentile);
    }

    //TODO remove
    private void printRequestAggregationResult(int requestNumber, Aggregation aggregation) {
        System.out.printf("👉 Request #%d Aggregation result:%n " + prettyPrint(aggregation) + "%n", requestNumber);
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

    private String generateOrderNumbers(int numberOfOrders) {
        List<String> numbers = IntStream.rangeClosed(1, numberOfOrders)
            .boxed()
            .map(String::valueOf)
            .toList();
        return String.join(",", numbers);
    }

    private String prettyPrint(Aggregation aggregation) {
        try {
            return JacksonConfiguration.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(aggregation);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
