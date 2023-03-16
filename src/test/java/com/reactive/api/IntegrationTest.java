package com.reactive.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.reactive.api.aggregation.Aggregation;
import com.reactive.api.config.ConfigProperties;
import com.reactive.api.config.JacksonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT2M")
class IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConfigProperties properties;

    @Test
    void whenNoRequestParams_expectAggregationIsEmpty() {
        Aggregation aggregation = webTestClient.get()
            .uri(getAggregationUrl(), null, null, null)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Aggregation.class)
            .returnResult()
            .getResponseBody();

        printAggregationResult(aggregation);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertTrue(aggregation.getShipments().isEmpty()),
            () -> assertTrue(aggregation.getTrack().isEmpty()),
            () -> assertTrue(aggregation.getPricing().isEmpty())
        );
    }

    @Test
    //TODO fails in suite
    void whenCallingAllAPIs_expectThemAllToRespond() {
        Aggregation aggregation = webTestClient.get()
            .uri(getAggregationUrl(), 1, 2, "AD")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Aggregation.class)
            .returnResult()
            .getResponseBody();

        printAggregationResult(aggregation);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertFalse(aggregation.getShipments().isEmpty()),
            () -> assertFalse(aggregation.getTrack().isEmpty()),
            () -> assertFalse(aggregation.getPricing().isEmpty())
        );
    }

    @Test
    void whenStressTestingPricingAPI_expectProcessingTimeIsWithinSLA() {

        String countryCodes = TestUtil.getCountryCodes(10);

        Object orderNumbers = null; // no call to the shipments and track APIs

        Aggregation aggregation = webTestClient.get()
            .uri(getAggregationUrl(), orderNumbers, orderNumbers, countryCodes)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Aggregation.class)
            .returnResult()
            .getResponseBody();

        printAggregationResult(aggregation);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertFalse(aggregation.getShipments().isEmpty()),
            () -> assertFalse(aggregation.getTrack().isEmpty()),
            () -> assertFalse(aggregation.getPricing().isEmpty())
        );
    }

    private void printAggregationResult(Aggregation aggregation) {
        System.out.printf("👉 Aggregation result:%n " + prettyPrint(aggregation));
    }

    private String getAggregationUrl() {
        return properties.getBaseUrl() + properties.getUrl();
    }

    private String prettyPrint(Aggregation aggregation) {
        try {
            return JacksonConfiguration.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(aggregation);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
