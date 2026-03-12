package com.example.config.properties;

import java.time.Duration;

/**
 * Common contract for REST client properties.
 * Sealed to restrict implementations to known service property records.
 */
public sealed interface ClientProperties
        permits PaymentsClientProperties, NotificationsClientProperties {

    String baseUrl();

    Duration connectTimeout();

    Duration readTimeout();
}
