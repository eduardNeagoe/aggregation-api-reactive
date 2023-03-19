package com.reactive.api;


import com.reactive.api.aggregation.Aggregation;
import com.reactive.api.config.ConfigProperties;
import com.reactive.api.util.TestUtil;
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
    void whenRequestHasNoParams_expectNoData() {
        Aggregation aggregation = requestAggregation(null, null, null);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertTrue(aggregation.getShipments().isEmpty()),
            () -> assertTrue(aggregation.getTrack().isEmpty()),
            () -> assertTrue(aggregation.getPricing().isEmpty())
        );
    }

    @Test
    void whenRequestHasAllParams_expectFullDataAggregation() {
        String orderNumbers = TestUtil.generateOrderNumbers(3);
        String countryCodes = TestUtil.getCountryCodes(3);

        Aggregation aggregation = requestAggregation(orderNumbers, orderNumbers, countryCodes);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertFalse(aggregation.getShipments().isEmpty()),
            () -> assertFalse(aggregation.getTrack().isEmpty()),
            () -> assertFalse(aggregation.getPricing().isEmpty())
        );
    }

    @Test
    void whenRequestHasOnlyShipmentsParams_expectOnlyShipmentsData() {
        String shipmentsOrderNumbers = TestUtil.generateOrderNumbers(10);

        Aggregation aggregation = requestAggregation(shipmentsOrderNumbers, null, null);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertTrue(aggregation.getPricing().isEmpty()),
            () -> assertFalse(aggregation.getShipments().isEmpty()),
            () -> assertTrue(aggregation.getTrack().isEmpty())
        );
    }

    @Test
    void whenRequestHasOnlyTrackParams_expectOnlyTrackData() {
        String trackOrderNumbers = TestUtil.generateOrderNumbers(10);

        Aggregation aggregation = requestAggregation(null, trackOrderNumbers, null);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertTrue(aggregation.getPricing().isEmpty()),
            () -> assertTrue(aggregation.getShipments().isEmpty()),
            () -> assertFalse(aggregation.getTrack().isEmpty())
        );
    }

    @Test
    void whenRequestHasOnlyPricingParams_expectOnlyPricingData() {
        String countryCodes = TestUtil.getCountryCodes(10);

        Aggregation aggregation = requestAggregation(null, null, countryCodes);

        assertAll(
            () -> assertNotNull(aggregation),
            () -> assertFalse(aggregation.getPricing().isEmpty()),
            () -> assertTrue(aggregation.getShipments().isEmpty()),
            () -> assertTrue(aggregation.getTrack().isEmpty())
        );
    }

    private Aggregation requestAggregation(String shipmentsOrderNumbers, String trackOrderNumbers, String pricingCountryCodes) {
        Aggregation aggregation = webTestClient.get()
            .uri(getAggregationUrl(), shipmentsOrderNumbers, trackOrderNumbers, pricingCountryCodes)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Aggregation.class)
            .returnResult()
            .getResponseBody();

        return aggregation;
    }

    private String getAggregationUrl() {
        return properties.getBaseUrl() + properties.getUrl();
    }

}
