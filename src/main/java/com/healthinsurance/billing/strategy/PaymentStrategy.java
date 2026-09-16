package com.healthinsurance.billing.strategy;

import com.healthinsurance.billing.dto.PaymentRequestDto;

import java.math.BigDecimal;

public interface PaymentStrategy {

    PaymentResult executePayment(BigDecimal amount, PaymentRequestDto requestDto);
}
