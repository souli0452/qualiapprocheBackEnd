package com.qualiapproche.referentiel.config;

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
import java.util.Collection;
import java.lang.reflect.ParameterizedType;

/**
 * Décodeur Feign personnalisé qui extrait automatiquement le champ "data"
 * de l'enveloppe ApiResponse produite par le GlobalResponseHandler.
 * Gère aussi le cas où data est une structure paginée mais qu'un objet simple est attendu.
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

                // Si le type attendu est un objet simple mais que le noeud data contient "content" (pagination)
                if (!isCollectionOrPage(type) && dataNode.has("content") && dataNode.get("content").isArray()) {
                    JsonNode contentNode = dataNode.get("content");
                    if (contentNode.size() > 0) {
                        dataNode = contentNode.get(0);
                        log.debug("Feign: Extracting first element from paginated content for type: {}", type.getTypeName());
                    } else {
                        log.debug("Feign: Empty paginated content for type: {}", type.getTypeName());
                        return null;
                    }
                }

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

    private boolean isCollectionOrPage(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            return Collection.class.isAssignableFrom(clazz) ||
                   clazz.isArray() ||
                   clazz.getName().contains("Page") ||
                   clazz.getName().contains("Slice");
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return isCollectionOrPage(rawType);
        }
        return false;
    }
}
