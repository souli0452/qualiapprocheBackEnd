package com.qualiapproche.evaluation.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Décodeur Feign qui défait l'enveloppe {@code ApiResponse}.
 *
 * <p>GlobalResponseHandler enveloppe toute réponse des services dans un champ {@code data} ;
 * sans ce décodeur, un client Feign recevrait l'enveloppe au lieu de la donnée attendue. Il
 * traite aussi le cas où la donnée demandée est un objet simple alors que le service l'a
 * paginée : le premier élément du {@code content} est alors retenu.</p>
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

                // Si le type attendu est un objet simple (ni Collection, ni Page)
                // mais que le noeud data contient un champ "content" (de type tableau)
                if (!isCollectionOrPage(type) && dataNode.has("content") && dataNode.get("content").isArray()) {
                    JsonNode contentNode = dataNode.get("content");
                    if (contentNode.size() > 0) {
                        dataNode = contentNode.get(0);
                        log.debug("Feign : extraction du premier élément du contenu paginé pour le type {}", type.getTypeName());
                    } else {
                        log.debug("Feign : contenu paginé vide pour le type {}", type.getTypeName());
                        return null;
                    }
                }

                log.debug("Feign : déenveloppage d'ApiResponse.data pour le type {}", type.getTypeName());
                JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                return objectMapper.convertValue(dataNode, javaType);
            }

            // Sinon, désérialisation directe (réponse non enveloppée)
            if (root != null) {
                JavaType javaType = objectMapper.getTypeFactory().constructType(type);
                return objectMapper.convertValue(root, javaType);
            }

            return null;
        } catch (Exception e) {
            log.error("Erreur de décodage Feign pour le type {} : {}", type.getTypeName(), e.getMessage());
            throw new DecodeException(response.status(), e.getMessage(), response.request(), e);
        }
    }

    private boolean isCollectionOrPage(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            return Collection.class.isAssignableFrom(clazz)
                    || clazz.isArray()
                    || clazz.getName().contains("Page")
                    || clazz.getName().contains("Slice");
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return isCollectionOrPage(rawType);
        }
        return false;
    }
}
