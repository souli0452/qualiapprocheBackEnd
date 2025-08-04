package com.qualiapproche.config;

import com.qualiapproche.config.utils.KcAuthProperties;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import javax.net.ssl.SSLException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final KcAuthProperties kcAuthProperties;

    TcpClient tcpClient = TcpClient.create()
            .secure(ssl -> {
                try {
                    ssl.sslContext(SslContextBuilder.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE) // Trust all certificates
                            .build());
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                }
            });

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .baseUrl(kcAuthProperties.getIssuerUri())
                .filter(errorHandler()) // This is an optional
                .filter(logRequest())
                .build();

    }

    private static ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError() || clientResponse.statusCode().is4xxClientError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new IllegalAccessException(errorBody)));
            } else {
                return Mono.just(clientResponse);
            }
        });
    }

    /**
     * Log request for debugging purposes
     */
    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, value)));

            return Mono.just(clientRequest);
        });
    }
}
