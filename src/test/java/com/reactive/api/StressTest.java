package com.reactive.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.reactive.api.aggregation.Aggregation;
import com.reactive.api.config.ConfigProperties;
import com.reactive.api.config.JacksonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT2M")
class StressTest {

    // 249 country codes
    private static final List<String> ALL_COUNTRIES = Arrays.stream(Locale.getISOCountries()).toList();
    private static final int ERROR_MARGIN_MILLIS = 1000;

    private double apisRequestDurationThreshold;

    private double aggregationRequestSLA;


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConfigProperties properties;

    @BeforeEach
    public void createContext() {
        // the SLA of the aggregation API
        aggregationRequestSLA = Long.valueOf(properties.getSla().toMillis()).doubleValue() / 1000;

        // 500 milliseconds above the timeout defined in the yaml config file for shipment, pricing, and tracking APIs
        apisRequestDurationThreshold = Long.valueOf(properties.getApisTimeout().plus(Duration.ofMillis(ERROR_MARGIN_MILLIS)).toMillis()).doubleValue() / 1000;
    }

    @Test
    void whenStressTestingPricingAPI_expectProcessingTimeIsWithinSLA() {

        // TODO set to 1000
        int numberOfRequests = 10;

        int numberOfCountriesPerRequest = 50;
        assert numberOfCountriesPerRequest <= ALL_COUNTRIES.size();

        String countryCodes = getCountryCodesAsString(numberOfCountriesPerRequest);
        Object orderNumbers = null; // we are only providing parameters for the pricing service

        StopWatch batchStopWatch = startNewStopWatch();

        IntStream.rangeClosed(1, numberOfRequests)
            .parallel()
            .forEach(requestNumber -> {

                StopWatch requestStopWatch = startNewStopWatch();

                Aggregation aggregation = webTestClient.get()
                    .uri(properties.getBaseUrl() + properties.getUrl(), orderNumbers, orderNumbers, countryCodes)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Aggregation.class)
                    .returnResult()
                    .getResponseBody();

                double requestDuration = getDurationInMilliSeconds(requestStopWatch);

                printDurationForCurrentRequest(requestNumber, requestDuration);
                printRequestAggregationResults(requestNumber, aggregation);

                double requestDurationInSeconds = requestDuration / 1000;

                assertAll(
                    () -> assertNotNull(aggregation),
                    () -> assertDurationAgainstThreshold(requestDurationInSeconds, apisRequestDurationThreshold,
                        "💀 Request duration was greater than the threshold of %s seconds. Actual value: %s")
                );

            });

        double totalDuration = getDurationInSeconds(batchStopWatch);

        System.out.printf("⏱️ Total Duration per %d Aggregation requests of %d countries each: %s seconds (%d total requests sent to the Pricing API)%n",
            numberOfRequests, numberOfCountriesPerRequest, totalDuration, numberOfRequests * numberOfCountriesPerRequest);

        assertDurationAgainstThreshold(totalDuration, aggregationRequestSLA * numberOfRequests, "💀 Request batch duration was greater than the SLA of %s seconds. Actual value: %s");
    }

    @Test
    void whenStressTestingAllAPIs_expectProcessingTimeIsWithinSLA() {

        // TODO set to 1000
        int numberOfRequests = 100;

        int numberOfCountriesPerRequest = ALL_COUNTRIES.size();
        String countryCodes = getCountryCodesAsString(numberOfCountriesPerRequest);

        int numberOfOrders = 300;
        String orderNumbers = getOrderNumbersAsString(numberOfOrders);

        StopWatch batchStopWatch = startNewStopWatch();

        IntStream.rangeClosed(1, numberOfRequests)
            .parallel()
            .forEach(requestNumber -> {

                StopWatch stopWatch = startNewStopWatch();

                Aggregation aggregation = webTestClient.get()
                    .uri(properties.getBaseUrl() + properties.getUrl(), orderNumbers, orderNumbers, countryCodes)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Aggregation.class)
                    .returnResult()
                    .getResponseBody();

                double requestDuration = getDurationInMilliSeconds(stopWatch);

                printDurationForCurrentRequest(requestNumber, requestDuration);
                printRequestAggregationResults(requestNumber, aggregation);

                double requestDurationInSeconds = requestDuration / 1000;

                assertAll(
                    () -> assertNotNull(aggregation),
                    () -> assertTrue(requestDurationInSeconds < apisRequestDurationThreshold,
                        "💀 Request duration was greater than %s seconds. Actual value: %s"
                            .formatted(apisRequestDurationThreshold, requestDurationInSeconds))
                );

            });

        double totalDuration = getDurationInSeconds(batchStopWatch);

        System.out.printf("⏱️ Total Duration per %d Aggregation requests of %d countries and %d order numbers each: %s seconds %n",
            numberOfRequests, numberOfCountriesPerRequest, numberOfOrders, totalDuration);

        assertDurationAgainstThreshold(totalDuration, aggregationRequestSLA * numberOfRequests,
            "💀 Request batch duration was greater than %s seconds (number of requests x SLA). Actual value: %s");
    }

    @Test
    void whenNoRequestParams_expectAggregationIsEmpty() {
        StopWatch stopWatch = startNewStopWatch();

        Aggregation aggregation = webTestClient.get()
            .uri(properties.getBaseUrl() + properties.getUrl(), null, null, null)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Aggregation.class)
            .returnResult()
            .getResponseBody();

        double requestDuration = getDurationInMilliSeconds(stopWatch);

        System.out.printf("👉 Aggregation result:%n " + prettyPrint(aggregation) + "%n");
        printRequestDuration(requestDuration);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertTrue(aggregation.getShipments().isEmpty()),
            () -> assertTrue(aggregation.getTrack().isEmpty()),
            () -> assertTrue(aggregation.getPricing().isEmpty())
        );
    }

    @Test
    //TODO fails in suite
    void whenSendingARequestToEachAPI_expectThemAllToRespond() {
        StopWatch stopWatch = startNewStopWatch();

        Aggregation aggregation = webTestClient.get()
            .uri(properties.getBaseUrl() + properties.getUrl(), 1, 2, "AD")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Aggregation.class)
            .returnResult()
            .getResponseBody();

        double requestDuration = getDurationInMilliSeconds(stopWatch);

        System.out.printf("👉 Aggregation result:%n " + prettyPrint(aggregation) + "%n");
        printRequestDuration(requestDuration);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertFalse(aggregation.getShipments().isEmpty()),
            () -> assertFalse(aggregation.getTrack().isEmpty()),
            () -> assertFalse(aggregation.getPricing().isEmpty())
        );
    }

    private void assertDurationAgainstThreshold(double totalDuration, double aggregationRequestSLA, String message) {
        assertTrue(totalDuration < aggregationRequestSLA,
            message.formatted(aggregationRequestSLA, totalDuration));
    }

    private void printDurationForCurrentRequest(int requestNumber, double requestDuration) {
        System.out.printf("⏱️ Duration per Aggregation request #%d: %s milliseconds%n", requestNumber, requestDuration);
    }

    private void printRequestDuration(double requestDuration) {
        System.out.printf("⏱️ Duration per Aggregation request: %s milliseconds %n", requestDuration);
    }

    private void printRequestAggregationResults(int requestNumber, Aggregation aggregation) {
        System.out.printf("👉 Request #%d Aggregation result:%n " + prettyPrint(aggregation) + "%n", requestNumber);
    }

    private String getCountryCodesAsString(int numberOfCountriesPerRequest) {
        return String.join(",", ALL_COUNTRIES.subList(0, numberOfCountriesPerRequest));
    }

    private StopWatch startNewStopWatch() {
        StopWatch batchStopWatch = new StopWatch();
        batchStopWatch.start();
        return batchStopWatch;
    }

    private double getDurationInSeconds(StopWatch requestStopWatch) {
        requestStopWatch.stop();
        return requestStopWatch.getTotalTimeSeconds();
    }

    private double getDurationInMilliSeconds(StopWatch requestStopWatch) {
        requestStopWatch.stop();
        return requestStopWatch.getTotalTimeMillis();
    }


    private String getOrderNumbersAsString(int numberOfOrders) {
        return String.join(",",
            IntStream.rangeClosed(1, numberOfOrders)
                .boxed()
                .map(String::valueOf)
                .toList()
        );
    }

    private String prettyPrint(Aggregation aggregation) {
        try {
            return JacksonConfiguration.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(aggregation);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
