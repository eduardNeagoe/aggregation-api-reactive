package com.reactive.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtil {

    // 249 country codes
    private static final List<String> ALL_COUNTRY_CODES = Arrays.stream(Locale.getISOCountries()).toList();

    public static String getCountryCodes(int numberOfCountriesPerRequest) {
        return String.join(",", ALL_COUNTRY_CODES.subList(0, numberOfCountriesPerRequest));
    }

    public static String getAllCountryCodes() {
        return String.join(",", ALL_COUNTRY_CODES);
    }
}
