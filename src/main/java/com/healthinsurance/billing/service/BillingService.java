package com.healthinsurance.billing.service;

import com.healthinsurance.billing.dto.PaymentReceiptDto;
import com.healthinsurance.billing.dto.PaymentRequestDto;
import com.healthinsurance.billing.dto.RefundRequestDto;

import java.util.List;

public interface BillingService {

    PaymentReceiptDto payPremium(String username, PaymentRequestDto requestDto);

    List<PaymentReceiptDto> getPaymentHistory(String username);

    byte[] downloadReceiptPdf(Long paymentId, String username);

    void sendPaymentReminders();

    RefundRequestDto requestRefund(String username, RefundRequestDto refundDto);

    List<RefundRequestDto> getAllRefundRequests();

    List<RefundRequestDto> getMyRefundRequests(String username);

    RefundRequestDto updateRefundStatus(Long refundId, String status, String processorUsername);

    RefundRequestDto updateRefundRequest(Long refundId, RefundRequestDto updateDto, String username);

    void deleteRefundRequest(Long refundId, String username);
}
