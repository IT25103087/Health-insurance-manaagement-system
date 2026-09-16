package com.healthinsurance.billing.strategy;

import com.healthinsurance.billing.dto.PaymentRequestDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("BANK_TRANSFER")
public class BankTransferPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult executePayment(BigDecimal amount, PaymentRequestDto requestDto) {
        String ref = "TXN-BT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(true, ref, "Bank direct transfer authorized");
    }
}
