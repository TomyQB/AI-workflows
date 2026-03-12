package com.example.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "client.notifications")
public record NotificationsClientProperties(
        @DefaultValue("http://localhost:8082") String baseUrl,
        @DefaultValue("PT5S") Duration connectTimeout,
        @DefaultValue("PT15S") Duration readTimeout
) implements ClientProperties {}
