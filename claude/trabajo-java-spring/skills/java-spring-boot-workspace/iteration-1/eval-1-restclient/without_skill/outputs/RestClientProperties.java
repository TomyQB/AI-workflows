package com.example.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rest-client")
public record RestClientProperties(
        ServiceProperties paymentsApi,
        ServiceProperties notificationsApi
) {

    public record ServiceProperties(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {

        public ServiceProperties {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl must not be null or blank");
            }
            if (connectTimeout == null) {
                connectTimeout = Duration.ofSeconds(5);
            }
            if (readTimeout == null) {
                readTimeout = Duration.ofSeconds(10);
            }
        }
    }
}
