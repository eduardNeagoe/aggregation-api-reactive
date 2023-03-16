package com.reactive.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    public static ObjectMapper MAPPER = new ObjectMapper();


    @Bean
    public ObjectMapper objectMapper() {
        MAPPER.registerModule(new Jdk8Module());
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return MAPPER;
    }
}
