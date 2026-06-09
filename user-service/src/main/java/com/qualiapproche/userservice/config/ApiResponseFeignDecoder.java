package com.qualiapproche.userservice.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Décodeur Feign personnalisé qui extrait automatiquement le champ "data"
 * de l'enveloppe ApiResponse produite par le GlobalResponseHandler.
 *
 * Ainsi, tous les appels Feign vers les microservices internes reçoivent
 * directement le bon type Java sans se soucier du wrapper.
 */
@Slf4j
public class ApiResponseFeignDecoder implements Decoder {

    private final ObjectMapper objectMapper;

    public ApiResponseFeignDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.body() == null) {
            return null;
        }

        try (InputStream inputStream = response.body().asInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);

            // Si la réponse est enveloppée par GlobalResponseHandler (champ "data" présent)
            if (root != null && root.has("data") && !root.get("data").isNull()) {
                JsonNode dataNode = root.get("data");
                log.debug("Feign: Unwrapping ApiResponse.data pour type: {}", type.getTypeName());
                JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                return objectMapper.convertValue(dataNode, javaType);
            }

            // Sinon, désérialisation directe (réponse non-wrappée)
            if (root != null) {
                JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                return objectMapper.convertValue(root, javaType);
            }

            return null;
        } catch (Exception e) {
            log.error("Feign decode error pour type {}: {}", type.getTypeName(), e.getMessage());
            throw new DecodeException(response.status(), e.getMessage(), response.request(), e);
        }
    }
}
