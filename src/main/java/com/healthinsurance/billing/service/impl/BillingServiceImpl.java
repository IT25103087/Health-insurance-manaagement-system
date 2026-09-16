package com.healthinsurance.billing.service.impl;

import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.billing.dto.PaymentReceiptDto;
import com.healthinsurance.billing.dto.PaymentRequestDto;
import com.healthinsurance.billing.dto.RefundRequestDto;
import com.healthinsurance.billing.entity.Payment;
import com.healthinsurance.billing.entity.RefundRequest;
import com.healthinsurance.billing.repository.PaymentRepository;
import com.healthinsurance.billing.repository.RefundRequestRepository;
import com.healthinsurance.billing.service.BillingService;
import com.healthinsurance.billing.strategy.PaymentResult;
import com.healthinsurance.billing.strategy.PaymentStrategy;
import com.healthinsurance.billing.strategy.PaymentStrategyFactory;
import com.healthinsurance.policy.entity.PolicyApplication;
import com.healthinsurance.policy.repository.PolicyApplicationRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingServiceImpl implements BillingService {

    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PolicyApplicationRepository policyRepository;
    private final UserRepository userRepository;
    private final PaymentStrategyFactory strategyFactory;

    public BillingServiceImpl(
            PaymentRepository paymentRepository,
            RefundRequestRepository refundRequestRepository,
            PolicyApplicationRepository policyRepository,
            UserRepository userRepository,
            PaymentStrategyFactory strategyFactory) {
        this.paymentRepository = paymentRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.policyRepository = policyRepository;
        this.userRepository = userRepository;
        this.strategyFactory = strategyFactory;
    }

    @Override
    @Transactional
    public PaymentReceiptDto payPremium(String username, PaymentRequestDto requestDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        PolicyApplication policy = policyRepository.findById(requestDto.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + requestDto.getPolicyId()));

        PaymentStrategy strategy = strategyFactory.getStrategy(requestDto.getPaymentMethod());
        PaymentResult result = strategy.executePayment(requestDto.getAmount(), requestDto);

        if (!result.isSuccessful()) {
            throw new IllegalArgumentException(result.getMessage());
        }

        Payment payment = new Payment();
        payment.setTransactionReference(result.getTransactionReference());
        payment.setCustomer(user);
        payment.setPolicy(policy);
        payment.setAmount(requestDto.getAmount());
        payment.setPaymentMethod(requestDto.getPaymentMethod());
        payment.setStatus("SUCCESS");

        Payment saved = paymentRepository.save(payment);

        // Mark policy active upon payment
        if ("PENDING".equals(policy.getStatus())) {
            policy.setStatus("ACTIVE");
            policyRepository.save(policy);
        }

        return mapPaymentToReceipt(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentReceiptDto> getPaymentHistory(String username) {
        List<Payment> list = paymentRepository.findByCustomerUsername(username);
        List<PaymentReceiptDto> result = new ArrayList<>();
        for (Payment p : list) {
            result.add(mapPaymentToReceipt(p));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadReceiptPdf(Long paymentId, String username) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Color brandNavy = new Color(15, 23, 42);       // #0F172A
            Color brandBlue = new Color(2, 132, 199);      // #0284C7
            Color emerald700 = new Color(4, 120, 87);      // #047857
            Color slate700 = new Color(51, 65, 85);        // #334155
            Color slate500 = new Color(100, 116, 139);     // #64748B
            Color slate200 = new Color(226, 232, 240);     // #E2E8F0
            Color slate50 = new Color(248, 250, 252);      // #F8FAFC
            Color headerBg = new Color(241, 245, 249);     // #F1F5F9

            Font brandTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, brandNavy);
            Font brandSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, emerald700);
            Font receiptTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, brandNavy);
            Font receiptNumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, emerald700);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, slate500);
            Font secHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, brandNavy);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, slate700);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, brandNavy);
            Font boldValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, brandNavy);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, slate500);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String paidAtStr = payment.getPaidAt() != null ? payment.getPaidAt().format(formatter) : "N/A";
            String paidYear = payment.getPaidAt() != null ? String.valueOf(payment.getPaidAt().getYear()) : "2026";
            String formattedAmount = String.format("%,.2f", payment.getAmount());

            PdfPTable headerTable = new PdfPTable(new float[]{60f, 40f});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(10);

            PdfPCell hLeft = new PdfPCell();
            hLeft.setBorder(Rectangle.NO_BORDER);
            hLeft.addElement(new Paragraph("HEALTHSHIELD", brandTitleFont));
            Paragraph subP = new Paragraph("BILLING & TREASURY DIVISION", brandSubFont);
            subP.setSpacingAfter(4);
            hLeft.addElement(subP);
            hLeft.addElement(new Paragraph("Level 14, West Tower, World Trade Center, Colombo 01, Sri Lanka", metaFont));
            hLeft.addElement(new Paragraph("Hotline: +94 11 255 6677  |  VAT Reg: LK-VAT-9928374-2026", metaFont));
            headerTable.addCell(hLeft);

            PdfPCell hRight = new PdfPCell();
            hRight.setBorder(Rectangle.BOX);
            hRight.setBorderColor(slate200);
            hRight.setBorderWidth(1f);
            hRight.setBackgroundColor(slate50);
            hRight.setPadding(8f);

            Paragraph rTitle = new Paragraph("PAYMENT RECEIPT", receiptTitleFont);
            rTitle.setAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(rTitle);

            Paragraph rNum = new Paragraph("Receipt #: RCT-" + payment.getId() + "-" + paidYear, receiptNumFont);
            rNum.setAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(rNum);

            Paragraph rStatus = new Paragraph("Settlement: " + payment.getStatus() + "  |  Date: " + (payment.getPaidAt() != null ? payment.getPaidAt().toLocalDate() : "2026-09-16"), metaFont);
            rStatus.setAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(rStatus);
            headerTable.addCell(hRight);

            document.add(headerTable);

            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(100);
            lineTable.setSpacingAfter(10);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(emerald700);
            lineCell.setFixedHeight(2.5f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineTable.addCell(lineCell);
            document.add(lineTable);

            Paragraph lead = new Paragraph(
                "This electronic receipt acknowledges official settlement of premium contributions under the "
                + "HealthShield Healthcare Protection Framework. Retain this receipt for policy renewal verification and statutory tax deductions.",
                FontFactory.getFont(FontFactory.HELVETICA, 8.5f, slate700)
            );
            lead.setSpacingAfter(12);
            document.add(lead);

            PdfPTable t1 = new PdfPTable(new float[]{32f, 68f});
            t1.setWidthPercentage(100);
            t1.setSpacingAfter(10);

            PdfPCell s1Header = new PdfPCell(new Phrase("1. PAYER & POLICY IDENTIFICATION", secHeaderFont));
            s1Header.setColspan(2);
            s1Header.setBackgroundColor(headerBg);
            s1Header.setBorderColor(slate200);
            s1Header.setPadding(5f);
            t1.addCell(s1Header);

            String custName = payment.getCustomer() != null ? payment.getCustomer().getFullName() : "N/A";
            String custUser = payment.getCustomer() != null ? payment.getCustomer().getUsername() : "N/A";
            String custEmail = payment.getCustomer() != null ? payment.getCustomer().getEmail() : "N/A";
            String polNum = payment.getPolicy() != null ? payment.getPolicy().getPolicyNumber() : "N/A";
            String planName = (payment.getPolicy() != null && payment.getPolicy().getPlan() != null)
                    ? payment.getPolicy().getPlan().getPlanName()
                    : "Comprehensive Health Plan";

            addTableRow(t1, "Insured Member Name", custName, labelFont, boldValueFont, slate200);
            addTableRow(t1, "Member Account ID", custUser, labelFont, valueFont, slate200);
            addTableRow(t1, "Registered Email", custEmail, labelFont, valueFont, slate200);
            addTableRow(t1, "Enrolled Policy Number", polNum, labelFont, boldValueFont, slate200);
            addTableRow(t1, "Insurance Plan Tier", planName, labelFont, valueFont, slate200);
            document.add(t1);

            PdfPTable t2 = new PdfPTable(new float[]{32f, 68f});
            t2.setWidthPercentage(100);
            t2.setSpacingAfter(10);

            PdfPCell s2Header = new PdfPCell(new Phrase("2. SETTLEMENT & TRANSACTION BREAKDOWN", secHeaderFont));
            s2Header.setColspan(2);
            s2Header.setBackgroundColor(headerBg);
            s2Header.setBorderColor(slate200);
            s2Header.setPadding(5f);
            t2.addCell(s2Header);

            addTableRow(t2, "Transaction Reference", payment.getTransactionReference(), labelFont, FontFactory.getFont(FontFactory.COURIER_BOLD, 8.5f, brandBlue), slate200);
            addTableRow(t2, "Payment Instrument", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "ONLINE_CARD", labelFont, valueFont, slate200);
            addTableRow(t2, "Processed Timestamp", paidAtStr, labelFont, valueFont, slate200);
            addTableRow(t2, "Payment Gateway Status", payment.getStatus() + " (Confirmed & Cleared)", labelFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, emerald700), slate200);
            addTableRow(t2, "Gross Premium Contribution", "Rs. " + formattedAmount + " LKR", labelFont, boldValueFont, slate200);
            addTableRow(t2, "Regulatory Stamp Duty & VAT", "Rs. 0.00 LKR (Exempt)", labelFont, valueFont, slate200);
            addTableRow(t2, "Total Amount Settled", "Rs. " + formattedAmount + " LKR", labelFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, emerald700), slate200);
            document.add(t2);

            PdfPTable t3 = new PdfPTable(new float[]{50f, 50f});
            t3.setWidthPercentage(100);
            t3.setSpacingAfter(12);

            PdfPCell s3Header = new PdfPCell(new Phrase("3. AUDIT & RECONCILIATION SEAL", secHeaderFont));
            s3Header.setColspan(2);
            s3Header.setBackgroundColor(headerBg);
            s3Header.setBorderColor(slate200);
            s3Header.setPadding(5f);
            t3.addCell(s3Header);

            PdfPCell leftSeal = new PdfPCell();
            leftSeal.setBorderColor(slate200);
            leftSeal.setPadding(7f);
            leftSeal.setBackgroundColor(slate50);
            leftSeal.addElement(new Paragraph("Gateway Settlement Hash:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, brandNavy)));
            leftSeal.addElement(new Paragraph("HS-PAY-" + Integer.toHexString(payment.getTransactionReference().hashCode()).toUpperCase() + "-SETTLED", FontFactory.getFont(FontFactory.COURIER, 8, emerald700)));
            leftSeal.addElement(new Paragraph("Ledger Confirmation: Transferred to HealthShield Escrow Account", metaFont));
            t3.addCell(leftSeal);

            PdfPCell rightSeal = new PdfPCell();
            rightSeal.setBorderColor(slate200);
            rightSeal.setPadding(7f);
            rightSeal.setBackgroundColor(slate50);
            rightSeal.addElement(new Paragraph("Authorized Treasury Officer:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, brandNavy)));
            rightSeal.addElement(new Paragraph("Kumara Senanayake, Head of Treasury Operations", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, slate700)));
            rightSeal.addElement(new Paragraph("HealthShield Insurance Corporation of Sri Lanka", metaFont));
            t3.addCell(rightSeal);

            document.add(t3);

            Paragraph foot = new Paragraph(
                "Thank you for your payment. Your insurance coverage is active and in good standing. "
                + "This receipt is an official system-generated financial document. Inquiries: billing@healthshield.lk.",
                footerFont
            );
            foot.setAlignment(Element.ALIGN_CENTER);
            document.add(foot);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating receipt PDF: " + e.getMessage());
        }

        return out.toByteArray();
    }

    @Override
    public void sendPaymentReminders() {
        // Scheduled task to find active policies due for renewal and send reminder notices
        List<PolicyApplication> active = policyRepository.findByStatus("ACTIVE");
        for (PolicyApplication p : active) {
            // Simulated email dispatch log
            System.out.println("Reminder sent for policy: " + p.getPolicyNumber() + " to " + p.getCustomer().getEmail());
        }
    }

    @Override
    @Transactional
    public RefundRequestDto requestRefund(String username, RefundRequestDto refundDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Payment payment = paymentRepository.findById(refundDto.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + refundDto.getPaymentId()));

        if (payment.getCustomer() == null || !username.equals(payment.getCustomer().getUsername())) {
            throw new IllegalArgumentException("You are not authorized to request a refund for this payment.");
        }

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalStateException("Only successful payments can be refunded.");
        }

        if (refundDto.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original paid amount: Rs. " + payment.getAmount());
        }

        BigDecimal committedRefunds = refundRequestRepository.findByPaymentId(payment.getId()).stream()
                .filter(existing -> !"REJECTED".equals(existing.getStatus()))
                .map(RefundRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (committedRefunds.add(refundDto.getAmount()).compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Total refund requests cannot exceed the original payment amount.");
        }

        RefundRequest req = new RefundRequest();
        req.setPayment(payment);
        req.setCustomer(user);
        req.setAmount(refundDto.getAmount());
        req.setReason(refundDto.getReason());
        req.setStatus("REQUESTED");

        RefundRequest saved = refundRequestRepository.save(req);
        return mapRefundToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundRequestDto> getAllRefundRequests() {
        List<RefundRequest> list = refundRequestRepository.findAll();
        List<RefundRequestDto> result = new ArrayList<>();
        for (RefundRequest r : list) {
            result.add(mapRefundToDto(r));
        }
        return result;
    }

    @Override
    @Transactional
    public RefundRequestDto updateRefundStatus(Long refundId, String status, String processorUsername) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("Refund status must be APPROVED or REJECTED.");
        }

        RefundRequest request = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + refundId));
        if (!"REQUESTED".equals(request.getStatus())) {
            throw new IllegalStateException("This refund request has already been processed.");
        }

        User processor = userRepository.findByUsername(processorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Processor not found: " + processorUsername));
        request.setStatus(status);
        request.setProcessedBy(processor);
        request.setProcessedAt(LocalDateTime.now());

        return mapRefundToDto(refundRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundRequestDto> getMyRefundRequests(String username) {
        List<RefundRequest> list = refundRequestRepository.findByCustomerUsername(username);
        List<RefundRequestDto> result = new ArrayList<>();
        for (RefundRequest r : list) {
            result.add(mapRefundToDto(r));
        }
        return result;
    }

    @Override
    @Transactional
    public RefundRequestDto updateRefundRequest(Long refundId, RefundRequestDto updateDto, String username) {
        RefundRequest request = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + refundId));

        if (!request.getCustomer().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to edit this refund request.");
        }
        if (!"REQUESTED".equals(request.getStatus())) {
            throw new IllegalStateException("Only a pending refund request can be edited.");
        }

        if (updateDto.getAmount() != null) {
            if (updateDto.getAmount().compareTo(request.getPayment().getAmount()) > 0) {
                throw new IllegalArgumentException("Refund amount cannot exceed original paid amount: Rs. " + request.getPayment().getAmount());
            }
            request.setAmount(updateDto.getAmount());
        }
        if (updateDto.getReason() != null && !updateDto.getReason().isBlank()) {
            request.setReason(updateDto.getReason());
        }

        RefundRequest saved = refundRequestRepository.save(request);
        return mapRefundToDto(saved);
    }

    @Override
    @Transactional
    public void deleteRefundRequest(Long refundId, String username) {
        RefundRequest request = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + refundId));

        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        boolean isOwner = request.getCustomer().getUsername().equals(username);
        boolean isAdmin = actor.getRole().name().equals("SYSTEM_ADMIN");

        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("You are not authorized to withdraw this refund request.");
        }
        // A customer may only withdraw their own request while it's still
        // pending; an admin may purge any old, already-processed record.
        if (isOwner && !isAdmin && !"REQUESTED".equals(request.getStatus())) {
            throw new IllegalStateException("You can only withdraw a refund request that is still pending.");
        }

        refundRequestRepository.delete(request);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color borderColor) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(5f);
        c1.setBackgroundColor(new Color(248, 250, 252));
        c1.setBorderColor(borderColor);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        c2.setPadding(5f);
        c2.setBorderColor(borderColor);

        table.addCell(c1);
        table.addCell(c2);
    }

    private PaymentReceiptDto mapPaymentToReceipt(Payment p) {
        return new PaymentReceiptDto(
                p.getId(),
                p.getTransactionReference(),
                p.getPolicy().getPolicyNumber(),
                p.getCustomer().getUsername(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.getPaidAt()
        );
    }

    private RefundRequestDto mapRefundToDto(RefundRequest r) {
        return new RefundRequestDto(
                r.getId(),
                r.getPayment().getId(),
                r.getAmount(),
                r.getReason(),
                r.getStatus(),
                r.getRequestedAt(),
                r.getProcessedBy() != null ? r.getProcessedBy().getUsername() : null,
                r.getProcessedAt()
        );
    }
}
