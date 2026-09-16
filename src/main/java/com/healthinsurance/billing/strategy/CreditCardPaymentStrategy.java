package com.healthinsurance.billing.strategy;

import com.healthinsurance.billing.dto.PaymentRequestDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("CREDIT_CARD")
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult executePayment(BigDecimal amount, PaymentRequestDto requestDto) {
        // Validate card number basic format
        if (requestDto.getCardNumber() != null && requestDto.getCardNumber().replace(" ", "").length() < 12) {
            return new PaymentResult(false, null, "Invalid credit card number format");
        }

        String ref = "TXN-CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(true, ref, "Card payment processed successfully");
    }
}
