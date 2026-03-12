package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentIntegrationService {

    @Qualifier("paymentsRestClient")
    private final RestClient paymentsRestClient;

    public PaymentResponse processPayment(final PaymentRequest request) {
        log.info("Processing payment for amount: {}", request.amount());
        return paymentsRestClient.post()
                .uri("/api/v1/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public PaymentResponse getPayment(final String paymentId) {
        log.info("Retrieving payment with id: {}", paymentId);
        return paymentsRestClient.get()
                .uri("/api/v1/payments/{id}", paymentId)
                .retrieve()
                .body(PaymentResponse.class);
    }

    /**
     * Example request DTO - in a real project this would be in its own file.
     */
    public record PaymentRequest(
            String orderId,
            java.math.BigDecimal amount,
            String currency
    ) {}

    /**
     * Example response DTO - in a real project this would be in its own file.
     */
    public record PaymentResponse(
            String paymentId,
            String status,
            java.math.BigDecimal amount,
            String currency
    ) {}
}
