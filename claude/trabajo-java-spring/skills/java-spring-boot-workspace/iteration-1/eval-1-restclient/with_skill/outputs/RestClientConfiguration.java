package com.example.config;

import com.example.config.properties.ClientProperties;
import com.example.config.properties.NotificationsClientProperties;
import com.example.config.properties.PaymentsClientProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfiguration {

    private final PaymentsClientProperties paymentsProperties;
    private final NotificationsClientProperties notificationsProperties;

    @Bean(name = "paymentsRestClient")
    RestClient paymentsRestClient() {
        return RestClient.builder()
                .baseUrl(paymentsProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(createRequestFactory(paymentsProperties))
                .build();
    }

    @Bean(name = "notificationsRestClient")
    RestClient notificationsRestClient() {
        return RestClient.builder()
                .baseUrl(notificationsProperties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(createRequestFactory(notificationsProperties))
                .build();
    }

    private ClientHttpRequestFactory createRequestFactory(final ClientProperties properties) {
        final var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
