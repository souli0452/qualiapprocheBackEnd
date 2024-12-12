package com.qualiapproche.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class JsonTools {

    /**
     * Simple Jackson ObjectMapper
     */
    public static final ObjectMapper DEFAULT_OBJECT_MAPPER = buildDefaultObjectMapper();

    /**
     * @return the default ObjectMapper
     */
    private static ObjectMapper buildDefaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
