package com.example.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "client.payments")
public record PaymentsClientProperties(
        @DefaultValue("http://localhost:8081") String baseUrl,
        @DefaultValue("PT5S") Duration connectTimeout,
        @DefaultValue("PT30S") Duration readTimeout
) implements ClientProperties {}
