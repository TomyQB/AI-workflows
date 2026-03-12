package com.example.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentService {

    private final RestClient paymentsApiRestClient;

    public PaymentService(@Qualifier("paymentsApiRestClient") RestClient paymentsApiRestClient) {
        this.paymentsApiRestClient = paymentsApiRestClient;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentsApiRestClient.post()
                .uri("/v1/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public PaymentResponse getPaymentStatus(String paymentId) {
        return paymentsApiRestClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public record PaymentRequest(
            String orderId,
            String currency,
            long amountInCents
    ) {}

    public record PaymentResponse(
            String paymentId,
            String status,
            String orderId
    ) {}
}
