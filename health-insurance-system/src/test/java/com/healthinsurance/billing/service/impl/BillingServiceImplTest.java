package com.healthinsurance.billing.service.impl;

import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.billing.dto.RefundRequestDto;
import com.healthinsurance.billing.entity.Payment;
import com.healthinsurance.billing.entity.RefundRequest;
import com.healthinsurance.billing.repository.PaymentRepository;
import com.healthinsurance.billing.repository.RefundRequestRepository;
import com.healthinsurance.billing.strategy.PaymentStrategyFactory;
import com.healthinsurance.policy.repository.PolicyApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRequestRepository refundRepository;
    @Mock private PolicyApplicationRepository policyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentStrategyFactory strategyFactory;

    private BillingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BillingServiceImpl(
                paymentRepository, refundRepository, policyRepository, userRepository, strategyFactory
        );
    }

    @Test
    void customerCannotRefundAnotherCustomersPayment() {
        User requester = user("kasun");
        Payment payment = payment(5L, user("nimal"), "10000.00");
        RefundRequestDto dto = refundDto(5L, "5000.00");

        when(userRepository.findByUsername("kasun")).thenReturn(Optional.of(requester));
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> service.requestRefund("kasun", dto));
        verify(refundRepository, never()).save(any(RefundRequest.class));
    }

    @Test
    void cumulativeRefundsCannotExceedPayment() {
        User requester = user("kasun");
        Payment payment = payment(5L, requester, "10000.00");
        RefundRequest existing = new RefundRequest();
        existing.setAmount(new BigDecimal("7000.00"));
        existing.setStatus("APPROVED");

        when(userRepository.findByUsername("kasun")).thenReturn(Optional.of(requester));
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(5L)).thenReturn(List.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.requestRefund("kasun", refundDto(5L, "4000.00"))
        );
    }

    @Test
    void processedRefundCannotBeProcessedAgain() {
        RefundRequest request = refundRequest("APPROVED");
        when(refundRepository.findById(8L)).thenReturn(Optional.of(request));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.updateRefundStatus(8L, "REJECTED", "support")
        );

        assertEquals("This refund request has already been processed.", error.getMessage());
    }

    @Test
    void requestedRefundCanBeApproved() {
        RefundRequest request = refundRequest("REQUESTED");
        User processor = user("support");
        when(refundRepository.findById(8L)).thenReturn(Optional.of(request));
        when(userRepository.findByUsername("support")).thenReturn(Optional.of(processor));
        when(refundRepository.save(request)).thenReturn(request);

        RefundRequestDto result = service.updateRefundStatus(8L, "APPROVED", "support");

        assertEquals("APPROVED", result.getStatus());
        assertEquals("support", result.getProcessedByUsername());
        verify(refundRepository).save(request);
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private Payment payment(Long id, User customer, String amount) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setCustomer(customer);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus("SUCCESS");
        return payment;
    }

    private RefundRequestDto refundDto(Long paymentId, String amount) {
        RefundRequestDto dto = new RefundRequestDto();
        dto.setPaymentId(paymentId);
        dto.setAmount(new BigDecimal(amount));
        dto.setReason("Billing error");
        return dto;
    }

    private RefundRequest refundRequest(String status) {
        User customer = user("kasun");
        RefundRequest request = new RefundRequest();
        request.setId(8L);
        request.setPayment(payment(5L, customer, "10000.00"));
        request.setCustomer(customer);
        request.setAmount(new BigDecimal("5000.00"));
        request.setReason("Billing error");
        request.setStatus(status);
        return request;
    }
}
