package com.qualiapproche.common.config;

import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.common.response.PaginatedResponse;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Apply to all controllers except those that already return ApiResponse (to avoid double wrapping)
        // Also exclude Swagger/OpenAPI endpoints if necessary.
        String className = returnType.getDeclaringClass().getName();
        if (className.contains("springdoc") || className.contains("swagger")) {
            return false;
        }

        Class<?> paramType = returnType.getParameterType();
        if (paramType.equals(ApiResponse.class) ||
            paramType.equals(byte[].class) ||
            Resource.class.isAssignableFrom(paramType)) {
            return false;
        }

        if (ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType) ||
            ResourceHttpMessageConverter.class.isAssignableFrom(converterType) ||
            ResourceRegionHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // If the body is already an ApiResponse, return it as is.
        if (body instanceof ApiResponse) {
            return body;
        }

        // If the body is a Spring Data Page, wrap it in PaginatedResponse first
        if (body instanceof Page) {
            PaginatedResponse<?> paginatedResponse = new PaginatedResponse<>((Page<?>) body);
            return ApiResponse.success(paginatedResponse);
        }

        // Automatic in-memory pagination for Lists
        if (body instanceof List) {
            List<?> list = (List<?>) body;

            int page = 0;
            int size = 10; // default size

            try {
                String pageParam = request.getURI().getQuery() != null ?
                    java.util.Arrays.stream(request.getURI().getQuery().split("&"))
                        .filter(s -> s.startsWith("page="))
                        .map(s -> s.substring(5))
                        .findFirst().orElse("0") : "0";

                String sizeParam = request.getURI().getQuery() != null ?
                    java.util.Arrays.stream(request.getURI().getQuery().split("&"))
                        .filter(s -> s.startsWith("size="))
                        .map(s -> s.substring(5))
                        .findFirst().orElse("10") : "10";

                page = Integer.parseInt(pageParam);
                size = Integer.parseInt(sizeParam);
            } catch (Exception e) {
                // Ignore parse errors, use defaults
            }

            int totalElements = list.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int start = Math.min(page * size, totalElements);
            int end = Math.min((page + 1) * size, totalElements);

            List<?> pagedList = list.subList(start, end);

            PaginatedResponse<Object> paginatedResponse = PaginatedResponse.builder()
                    .content((List<Object>) pagedList)
                    .pageNumber(page)
                    .pageSize(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .isLast(page >= totalPages - 1)
                    .build();

            return ApiResponse.success(paginatedResponse);
        }

        // Handle String return types carefully because StringHttpMessageConverter expects a String, not an Object.
        // Spring has a known issue when ResponseBodyAdvice returns an object for a String return type.
        if (body instanceof String) {
            // It's tricky to wrap Strings directly without custom configuration or Jackson mapping.
            // For simplicity, we can let Strings pass through, or serialize it manually.
            // Returning the string directly to avoid ClassCastException in StringHttpMessageConverter
            return body;
        }

        return ApiResponse.success(body);
    }
}
