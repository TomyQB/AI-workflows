package com.example.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientConfig {

    private final RestClientProperties properties;

    public RestClientConfig(RestClientProperties properties) {
        this.properties = properties;
    }

    @Bean("paymentsApiRestClient")
    public RestClient paymentsApiRestClient() {
        RestClientProperties.ServiceProperties paymentsProps = properties.paymentsApi();
        return RestClient.builder()
                .baseUrl(paymentsProps.baseUrl())
                .requestFactory(buildRequestFactory(paymentsProps.connectTimeout(), paymentsProps.readTimeout()))
                .build();
    }

    @Bean("notificationsApiRestClient")
    public RestClient notificationsApiRestClient() {
        RestClientProperties.ServiceProperties notificationsProps = properties.notificationsApi();
        return RestClient.builder()
                .baseUrl(notificationsProps.baseUrl())
                .requestFactory(buildRequestFactory(notificationsProps.connectTimeout(), notificationsProps.readTimeout()))
                .build();
    }

    private ClientHttpRequestFactory buildRequestFactory(Duration connectTimeout, Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);
        return ClientHttpRequestFactories.get(settings);
    }
}
