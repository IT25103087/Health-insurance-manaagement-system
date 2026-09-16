package com.healthinsurance.billing.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResult {
    private final boolean successful;
    private final String transactionReference;
    private final String message;
}
