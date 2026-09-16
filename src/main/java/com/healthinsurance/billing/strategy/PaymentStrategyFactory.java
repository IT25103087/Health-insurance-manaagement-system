package com.healthinsurance.billing.strategy;

import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    private final CreditCardPaymentStrategy creditCardStrategy;
    private final BankTransferPaymentStrategy bankTransferStrategy;

    public PaymentStrategyFactory(
            CreditCardPaymentStrategy creditCardStrategy,
            BankTransferPaymentStrategy bankTransferStrategy) {
        this.creditCardStrategy = creditCardStrategy;
        this.bankTransferStrategy = bankTransferStrategy;
    }

    public PaymentStrategy getStrategy(String paymentMethod) {
        if (paymentMethod != null && paymentMethod.equalsIgnoreCase("BANK_TRANSFER")) {
            return bankTransferStrategy;
        }
        // Default to credit card payment strategy
        return creditCardStrategy;
    }
}
