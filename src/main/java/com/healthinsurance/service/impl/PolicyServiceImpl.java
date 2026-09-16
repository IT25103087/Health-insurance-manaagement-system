package com.healthinsurance.policy.service.impl;

import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.policy.dto.PolicyApplicationDto;
import com.healthinsurance.policy.dto.PolicyPlanDto;
import com.healthinsurance.policy.entity.PolicyApplication;
import com.healthinsurance.policy.entity.PolicyPlan;
import com.healthinsurance.policy.repository.PolicyApplicationRepository;
import com.healthinsurance.policy.repository.PolicyPlanRepository;
import com.healthinsurance.policy.service.PolicyService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final PolicyPlanRepository planRepository;
    private final PolicyApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public PolicyServiceImpl(
            PolicyPlanRepository planRepository,
            PolicyApplicationRepository applicationRepository,
            UserRepository userRepository) {
        this.planRepository = planRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyPlanDto> getAllActivePlans() {
        List<PolicyPlan> plans = planRepository.findByActiveTrue();
        List<PolicyPlanDto> result = new ArrayList<>();
        for (PolicyPlan p : plans) {
            result.add(mapPlanToDto(p));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyPlanDto getPlanById(Long planId) {
        PolicyPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        return mapPlanToDto(plan);
    }

    @Override
    @Transactional
    public PolicyApplicationDto applyForPolicy(String username, PolicyApplicationDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        PolicyPlan plan = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + dto.getPlanId()));

        PolicyApplication app = new PolicyApplication();
        // Generate a random 6-digit policy number
        int randomNum = 100000 + new Random().nextInt(900000);
        app.setPolicyNumber("POL-" + randomNum);
        app.setCustomer(user);
        app.setPlan(plan);
        app.setStatus("ACTIVE");
        app.setStartDate(LocalDate.now());
        app.setEndDate(LocalDate.now().plusMonths(plan.getDurationMonths()));

        PolicyApplication saved = applicationRepository.save(app);
        return mapAppToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyApplicationDto> getCustomerPolicies(String username) {
        List<PolicyApplication> list = applicationRepository.findByCustomerUsername(username);
        List<PolicyApplicationDto> result = new ArrayList<>();
        for (PolicyApplication a : list) {
            result.add(mapAppToDto(a));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePolicyPdf(Long policyId) {
        PolicyApplication policy = applicationRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Color brandNavy = new Color(15, 23, 42);      // #0F172A
            Color brandBlue = new Color(2, 132, 199);     // #0284C7
            Color slate700 = new Color(51, 65, 85);       // #334155
            Color slate500 = new Color(100, 116, 139);    // #64748B
            Color slate200 = new Color(226, 232, 240);    // #E2E8F0
            Color slate50 = new Color(248, 250, 252);     // #F8FAFC
            Color emerald700 = new Color(4, 120, 87);     // #047857
            Color headerBg = new Color(241, 245, 249);    // #F1F5F9

            Font brandTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, brandNavy);
            Font brandSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, brandBlue);
            Font certTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, brandNavy);
            Font certNumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, brandBlue);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, slate500);
            Font secHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, brandNavy);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, slate700);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, brandNavy);
            Font boldValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, brandNavy);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, slate500);

            PdfPTable headerTable = new PdfPTable(new float[]{60f, 40f});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(10);

            PdfPCell hLeft = new PdfPCell();
            hLeft.setBorder(Rectangle.NO_BORDER);
            hLeft.addElement(new Paragraph("HEALTHSHIELD", brandTitleFont));
            Paragraph subP = new Paragraph("HEALTH INSURANCE CORPORATION", brandSubFont);
            subP.setSpacingAfter(4);
            hLeft.addElement(subP);
            hLeft.addElement(new Paragraph("Level 14, West Tower, World Trade Center, Colombo 01, Sri Lanka", metaFont));
            hLeft.addElement(new Paragraph("Hotline: +94 11 255 6677  |  Web: www.healthshield.lk  |  Reg: IRCSL/INS/2026", metaFont));
            headerTable.addCell(hLeft);

            PdfPCell hRight = new PdfPCell();
            hRight.setBorder(Rectangle.BOX);
            hRight.setBorderColor(slate200);
            hRight.setBorderWidth(1f);
            hRight.setBackgroundColor(slate50);
            hRight.setPadding(8f);

            Paragraph certTitle = new Paragraph("OFFICIAL POLICY CERTIFICATE", certTitleFont);
            certTitle.setAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(certTitle);

            Paragraph pNum = new Paragraph("Ref: " + policy.getPolicyNumber(), certNumFont);
            pNum.setAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(pNum);

            Paragraph pStatus = new Paragraph("Status: " + policy.getStatus() + "  |  Inception: " + policy.getStartDate(), metaFont);
            pStatus.setAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(pStatus);
            headerTable.addCell(hRight);

            document.add(headerTable);

            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(100);
            lineTable.setSpacingAfter(10);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(brandBlue);
            lineCell.setFixedHeight(2.5f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineTable.addCell(lineCell);
            document.add(lineTable);

            Paragraph lead = new Paragraph(
                "This document certifies that the individual named below is formally enrolled as an insured policyholder under "
                + "the HealthShield Healthcare Protection Framework. Coverage benefits and reimbursement claims are governed by "
                + "the policy schedule, terms, and conditions outlined below.",
                FontFactory.getFont(FontFactory.HELVETICA, 8.5f, slate700)
            );
            lead.setSpacingAfter(12);
            document.add(lead);

            PdfPTable t1 = new PdfPTable(new float[]{32f, 68f});
            t1.setWidthPercentage(100);
            t1.setSpacingAfter(10);

            PdfPCell s1Header = new PdfPCell(new Phrase("1. POLICYHOLDER IDENTIFICATION", secHeaderFont));
            s1Header.setColspan(2);
            s1Header.setBackgroundColor(headerBg);
            s1Header.setBorderColor(slate200);
            s1Header.setPadding(5f);
            t1.addCell(s1Header);

            addTableRow(t1, "Legal Full Name", policy.getCustomer().getFullName(), labelFont, boldValueFont, slate200);
            addTableRow(t1, "Insured Username", policy.getCustomer().getUsername(), labelFont, valueFont, slate200);
            addTableRow(t1, "Registered Email", policy.getCustomer().getEmail(), labelFont, valueFont, slate200);
            addTableRow(t1, "Contact Telephone", policy.getCustomer().getPhoneNumber() != null ? policy.getCustomer().getPhoneNumber() : "+94 71 345 6789", labelFont, valueFont, slate200);
            addTableRow(t1, "Account System ID", "HS-CUST-00" + policy.getCustomer().getId(), labelFont, valueFont, slate200);
            document.add(t1);

            PdfPTable t2 = new PdfPTable(new float[]{32f, 68f});
            t2.setWidthPercentage(100);
            t2.setSpacingAfter(10);

            PdfPCell s2Header = new PdfPCell(new Phrase("2. COVERAGE SCHEDULE & FINANCIAL BENEFIT LIMITS", secHeaderFont));
            s2Header.setColspan(2);
            s2Header.setBackgroundColor(headerBg);
            s2Header.setBorderColor(slate200);
            s2Header.setPadding(5f);
            t2.addCell(s2Header);

            String covAmt = String.format("%,.2f", policy.getPlan().getCoverageAmount());
            String premAmt = String.format("%,.2f", policy.getPlan().getMonthlyPremium());

            addTableRow(t2, "Insurance Plan Tier", policy.getPlan().getPlanName(), labelFont, boldValueFont, slate200);
            addTableRow(t2, "Maximum Annual Benefit Limit", "Rs. " + covAmt + " LKR", labelFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, emerald700), slate200);
            addTableRow(t2, "Monthly Premium", "Rs. " + premAmt + " LKR / month", labelFont, valueFont, slate200);
            addTableRow(t2, "Coverage Term", policy.getPlan().getDurationMonths() + " Months (" + policy.getStartDate() + " to " + policy.getEndDate() + ")", labelFont, valueFont, slate200);
            addTableRow(t2, "Cashless Hospital Network", "Full access across 120+ accredited nationwide partner hospitals", labelFont, valueFont, slate200);
            addTableRow(t2, "Reimbursement Settlement", "Expedited 48-hour adjudication for approved medical claims", labelFont, valueFont, slate200);
            addTableRow(t2, "Adjudication Status", policy.getStatus() + " (Active in Good Standing)", labelFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, emerald700), slate200);
            document.add(t2);

            PdfPTable t3 = new PdfPTable(new float[]{50f, 50f});
            t3.setWidthPercentage(100);
            t3.setSpacingAfter(12);

            PdfPCell s3Header = new PdfPCell(new Phrase("3. VERIFICATION & UNDERWRITING ENDORSEMENT", secHeaderFont));
            s3Header.setColspan(2);
            s3Header.setBackgroundColor(headerBg);
            s3Header.setBorderColor(slate200);
            s3Header.setPadding(5f);
            t3.addCell(s3Header);

            PdfPCell leftEndorse = new PdfPCell();
            leftEndorse.setBorderColor(slate200);
            leftEndorse.setPadding(7f);
            leftEndorse.setBackgroundColor(slate50);
            leftEndorse.addElement(new Paragraph("Electronic Verification Reference:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, brandNavy)));
            leftEndorse.addElement(new Paragraph("HS-VERIFY-" + Integer.toHexString(policy.getPolicyNumber().hashCode()).toUpperCase() + "-LK2026", FontFactory.getFont(FontFactory.COURIER, 8, brandBlue)));
            leftEndorse.addElement(new Paragraph("Registry Status: Digitally registered on HealthShield Central Ledger", metaFont));
            t3.addCell(leftEndorse);

            PdfPCell rightEndorse = new PdfPCell();
            rightEndorse.setBorderColor(slate200);
            rightEndorse.setPadding(7f);
            rightEndorse.setBackgroundColor(slate50);
            rightEndorse.addElement(new Paragraph("Authorized Underwriting Officer:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, brandNavy)));
            rightEndorse.addElement(new Paragraph("Sunil Jayawardena, Chief Underwriting Officer", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, slate700)));
            rightEndorse.addElement(new Paragraph("HealthShield Insurance Corporation of Sri Lanka", metaFont));
            t3.addCell(rightEndorse);

            document.add(t3);

            Paragraph foot = new Paragraph(
                "This certificate is an official legal record generated electronically by the HealthShield Health Insurance Management System. "
                + "No physical signature is required. For verification or claim assistance, contact support@healthshield.lk or visit www.healthshield.lk.",
                footerFont
            );
            foot.setAlignment(Element.ALIGN_CENTER);
            document.add(foot);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating policy PDF: " + e.getMessage());
        }

        return out.toByteArray();
    }

    @Override
    @Transactional
    public PolicyApplicationDto renewPolicy(Long policyId, String username) {
        PolicyApplication policy = applicationRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        if (!policy.getCustomer().getUsername().equals(username)) {
            // Check if agent/admin
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || (!user.getRole().name().equals("INSURANCE_AGENT") && !user.getRole().name().equals("SYSTEM_ADMIN"))) {
                throw new IllegalArgumentException("Unauthorized to renew this policy");
            }
        }

        // Extend by another 12 months
        policy.setEndDate(policy.getEndDate().plusMonths(12));
        policy.setStatus("RENEWED");
        PolicyApplication saved = applicationRepository.save(policy);
        return mapAppToDto(saved);
    }

    @Override
    @Transactional
    public PolicyApplicationDto cancelPolicy(Long policyId, String username) {
        PolicyApplication policy = applicationRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        policy.setStatus("CANCELLED");
        PolicyApplication saved = applicationRepository.save(policy);
        return mapAppToDto(saved);
    }

    @Override
    @Transactional
    public PolicyApplicationDto updatePolicy(Long policyId, PolicyApplicationDto updateDto, String username) {
        PolicyApplication policy = applicationRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        boolean isOwner = policy.getCustomer().getUsername().equals(username);
        if (!isOwner) {
            // Only an agent or admin may edit a policy that isn't their own
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || (!user.getRole().name().equals("INSURANCE_AGENT") && !user.getRole().name().equals("SYSTEM_ADMIN"))) {
                throw new IllegalArgumentException("Unauthorized to edit this policy application.");
            }
        }

        if ("CANCELLED".equals(policy.getStatus())) {
            throw new IllegalStateException("A cancelled policy application cannot be edited.");
        }

        // Allow switching to a different plan (e.g. upgrading/downgrading coverage)
        if (updateDto.getPlanId() != null && !updateDto.getPlanId().equals(policy.getPlan().getId())) {
            PolicyPlan newPlan = planRepository.findById(updateDto.getPlanId())
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + updateDto.getPlanId()));
            policy.setPlan(newPlan);
            policy.setEndDate(policy.getStartDate().plusMonths(newPlan.getDurationMonths()));
        }

        PolicyApplication saved = applicationRepository.save(policy);
        return mapAppToDto(saved);
    }

    @Override
    @Transactional
    public void deletePolicy(Long policyId, String username) {
        PolicyApplication policy = applicationRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (!user.getRole().name().equals("INSURANCE_AGENT") && !user.getRole().name().equals("SYSTEM_ADMIN")) {
            throw new IllegalArgumentException("Only an insurance agent or admin may permanently delete a policy record.");
        }

        // Active policies may have claims tied to them, so only a cancelled
        // application (customer already went through /cancel first) can be
        // permanently removed. This keeps claim history intact.
        if (!"CANCELLED".equals(policy.getStatus())) {
            throw new IllegalStateException("Only a cancelled policy application can be permanently deleted. Cancel it first.");
        }

        applicationRepository.delete(policy);
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

    private PolicyPlanDto mapPlanToDto(PolicyPlan p) {
        return new PolicyPlanDto(
                p.getId(),
                p.getPlanName(),
                p.getDescription(),
                p.getCoverageAmount(),
                p.getMonthlyPremium(),
                p.getDurationMonths(),
                p.isActive()
        );
    }

    private PolicyApplicationDto mapAppToDto(PolicyApplication a) {
        return new PolicyApplicationDto(
                a.getId(),
                a.getPolicyNumber(),
                a.getPlan().getId(),
                a.getPlan().getPlanName(),
                a.getCustomer().getId(),
                a.getCustomer().getUsername(),
                a.getStatus(),
                a.getStartDate(),
                a.getEndDate(),
                a.getAppliedAt()
        );
    }
}
