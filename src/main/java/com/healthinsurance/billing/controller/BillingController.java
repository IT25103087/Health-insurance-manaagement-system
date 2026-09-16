package com.healthinsurance.billing.controller;

import com.healthinsurance.billing.dto.PaymentReceiptDto;
import com.healthinsurance.billing.dto.PaymentRequestDto;
import com.healthinsurance.billing.dto.RefundRequestDto;
import com.healthinsurance.billing.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // Create
    @PostMapping("/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentReceiptDto> payPremium(
            @Valid @RequestBody PaymentRequestDto requestDto,
            Authentication authentication) {
        PaymentReceiptDto receipt = billingService.payPremium(authentication.getName(), requestDto);
        return new ResponseEntity<>(receipt, HttpStatus.CREATED);
    }

    // Read
    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentReceiptDto>> getPaymentHistory(Authentication authentication) {
        return ResponseEntity.ok(billingService.getPaymentHistory(authentication.getName()));
    }

    // Read
    @GetMapping("/receipts/{paymentId}/pdf")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('OPERATIONS_MANAGER')")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable Long paymentId, Authentication authentication) {
        byte[] pdfBytes = billingService.downloadReceiptPdf(paymentId, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + paymentId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Create
    @PostMapping("/refunds")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RefundRequestDto> requestRefund(
            @Valid @RequestBody RefundRequestDto refundDto,
            Authentication authentication) {
        RefundRequestDto created = billingService.requestRefund(authentication.getName(), refundDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Read
    @GetMapping("/refunds")
    @PreAuthorize("hasRole('OPERATIONS_MANAGER') or hasRole('SUPPORT_REP') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<RefundRequestDto>> getAllRefunds() {
        return ResponseEntity.ok(billingService.getAllRefundRequests());
    }

    // Read (a customer's own refund requests - lets the history page show a
    // list to edit/delete, since the endpoint above is staff-only)
    @GetMapping("/refunds/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<RefundRequestDto>> getMyRefunds(Authentication authentication) {
        return ResponseEntity.ok(billingService.getMyRefundRequests(authentication.getName()));
    }

    // Update (status transition)
    @PostMapping("/refunds/{id}/approve")
    @PreAuthorize("hasRole('OPERATIONS_MANAGER') or hasRole('SUPPORT_REP') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RefundRequestDto> approveRefund(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(billingService.updateRefundStatus(id, "APPROVED", authentication.getName()));
    }

    // Update (status transition)
    @PostMapping("/refunds/{id}/reject")
    @PreAuthorize("hasRole('OPERATIONS_MANAGER') or hasRole('SUPPORT_REP') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RefundRequestDto> rejectRefund(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(billingService.updateRefundStatus(id, "REJECTED", authentication.getName()));
    }

    // Update
    @PutMapping("/refunds/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RefundRequestDto> updateRefund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequestDto updateDto,
            Authentication authentication) {
        return ResponseEntity.ok(billingService.updateRefundRequest(id, updateDto, authentication.getName()));
    }

    // Delete
    @DeleteMapping("/refunds/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteRefund(@PathVariable Long id, Authentication authentication) {
        billingService.deleteRefundRequest(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
