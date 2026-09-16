package com.healthinsurance.policy.util;

import com.healthinsurance.auth.entity.AuditLog;
import com.healthinsurance.auth.entity.Role;
import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.AuditLogRepository;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.billing.entity.Payment;
import com.healthinsurance.billing.repository.PaymentRepository;
import com.healthinsurance.claims.entity.Claim;
import com.healthinsurance.claims.entity.ClaimDocument;
import com.healthinsurance.claims.repository.ClaimDocumentRepository;
import com.healthinsurance.claims.repository.ClaimRepository;
import com.healthinsurance.claimsreview.entity.ClaimReview;
import com.healthinsurance.claimsreview.repository.ClaimReviewRepository;
import com.healthinsurance.policy.entity.PolicyApplication;
import com.healthinsurance.policy.entity.PolicyPlan;
import com.healthinsurance.policy.repository.PolicyApplicationRepository;
import com.healthinsurance.policy.repository.PolicyPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class PolicyDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyDataInitializer.class);

    private final UserRepository userRepository;
    private final PolicyPlanRepository planRepository;
    private final PolicyApplicationRepository applicationRepository;
    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final ClaimReviewRepository claimReviewRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public PolicyDataInitializer(
            UserRepository userRepository,
            PolicyPlanRepository planRepository,
            PolicyApplicationRepository applicationRepository,
            ClaimRepository claimRepository,
            ClaimDocumentRepository claimDocumentRepository,
            ClaimReviewRepository claimReviewRepository,
            PaymentRepository paymentRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.applicationRepository = applicationRepository;
        this.claimRepository = claimRepository;
        this.claimDocumentRepository = claimDocumentRepository;
        this.claimReviewRepository = claimReviewRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing Sri Lankan health insurance system seed data...");

        // Clean up legacy or intermediate usernames if present
        cleanUpLegacyUser("sunil.admin");
        cleanUpLegacyUser("kasun.perera");
        cleanUpLegacyUser("nimal.fernando");
        cleanUpLegacyUser("dilshan.silva");
        cleanUpLegacyUser("priyani.officer");
        cleanUpLegacyUser("kanchana.hospital");
        cleanUpLegacyUser("rohan.manager");
        cleanUpLegacyUser("chamara.agent");
        cleanUpLegacyUser("tharushi.support");
        cleanUpLegacyUser("customer1");
        cleanUpLegacyUser("admin1");

        // Seed initial users
        User admin = seedUser("admin", "admin@healthshield.lk", "Admin#Pass2026", "Sunil Jayawardena", "+94 77 234 5678", Role.SYSTEM_ADMIN);
        User customer = seedUser("customer", "kasun.perera@gmail.com", "Kasun#Pass2026", "Kasun Perera", "+94 71 345 6789", Role.CUSTOMER);
        User nimal = seedUser("nimal", "nimal.fernando@yahoo.com", "Nimal#Pass2026", "Nimal Fernando", "+94 76 456 7890", Role.CUSTOMER);
        User dilshan = seedUser("dilshan", "dilshan.silva@outlook.com", "Dilshan#Pass2026", "Dilshan Silva", "+94 78 567 8901", Role.CUSTOMER);
        User officer = seedUser("officer", "officer@healthshield.lk", "Claims#Pass2026", "Priyani Senaratne", "+94 72 789 0123", Role.CLAIMS_OFFICER);
        User hospital = seedUser("hospital", "hospital@asirihealth.lk", "Asiri#Pass2026", "Dr. Kanchana Alwis", "+94 11 452 3300", Role.HOSPITAL_OFFICER);
        User manager = seedUser("manager", "manager@healthshield.lk", "Ops#Pass2026", "Rohan Wickramasinghe", "+94 77 890 1234", Role.OPERATIONS_MANAGER);
        User agent = seedUser("agent", "chamara.agent@healthshield.lk", "Agent#Pass2026", "Chamara Bandara", "+94 70 678 9012", Role.INSURANCE_AGENT);
        User support = seedUser("support", "support@healthshield.lk", "Support#Pass2026", "Tharushi De Silva", "+94 11 255 6677", Role.SUPPORT_REP);

        // Clean up legacy test claims, payments, and policies
        claimRepository.findAll().stream()
                .filter(c -> c.getClaimNumber() != null && !c.getClaimNumber().startsWith("CLM-LK-"))
                .forEach(c -> {
                    try {
                        claimDocumentRepository.deleteAll(claimDocumentRepository.findByClaimId(c.getId()));
                        claimReviewRepository.deleteAll(claimReviewRepository.findByClaimIdOrderByReviewedAtDesc(c.getId()));
                        claimRepository.delete(c);
                    } catch (Exception ignored) {}
                });

        paymentRepository.findAll().stream()
                .filter(p -> p.getTransactionReference() != null && !p.getTransactionReference().startsWith("TXN-LK-"))
                .forEach(p -> {
                    try { paymentRepository.delete(p); } catch (Exception ignored) {}
                });

        applicationRepository.findAll().stream()
                .filter(a -> a.getPolicyNumber() != null && !a.getPolicyNumber().startsWith("POL-LK-"))
                .forEach(a -> {
                    try { applicationRepository.delete(a); } catch (Exception ignored) {}
                });

        // Deactivate old legacy plans if present
        for (PolicyPlan p : planRepository.findAll()) {
            if (p.getPlanName().equalsIgnoreCase("Basic Essential Health")
                    || p.getPlanName().equalsIgnoreCase("Silver Care Comprehensive")
                    || p.getPlanName().equalsIgnoreCase("Platinum Family Shield")) {
                p.setActive(false);
                planRepository.save(p);
            }
        }

        // Policy plans
        PolicyPlan plan1 = seedPlan(
                "Suwasetha Silver Individual",
                "Essential hospitalization and emergency surgical cover across registered private hospitals in Sri Lanka.",
                new BigDecimal("750000.00"),
                new BigDecimal("3500.00"),
                12
        );

        PolicyPlan plan2 = seedPlan(
                "Arogya Gold Family Shield",
                "Complete family health protection covering inpatient care, intensive care, specialist consultations, and emergency treatments.",
                new BigDecimal("2500000.00"),
                new BigDecimal("8500.00"),
                12
        );

        PolicyPlan plan3 = seedPlan(
                "Suwa Diriya Senior Comprehensive",
                "Dedicated healthcare security for seniors covering critical illnesses, routine screenings, and cataract surgeries.",
                new BigDecimal("1500000.00"),
                new BigDecimal("6200.00"),
                12
        );

        PolicyPlan plan4 = seedPlan(
                "Corporate Executive Care",
                "Premium corporate executive cover with cashless treatment at Asiri, Nawaloka, Lanka Hospitals, and Durdans.",
                new BigDecimal("5000000.00"),
                new BigDecimal("14000.00"),
                12
        );

        // Policy applications
        PolicyApplication app1 = seedPolicyApplication("POL-LK-2026-001", customer, plan2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        PolicyApplication app2 = seedPolicyApplication("POL-LK-2026-002", nimal, plan1, LocalDate.of(2026, 2, 15), LocalDate.of(2027, 2, 14));
        PolicyApplication app3 = seedPolicyApplication("POL-LK-2026-003", dilshan, plan4, LocalDate.of(2026, 3, 1), LocalDate.of(2027, 2, 28));

        // Hospital claims and documents
        Claim claim1 = seedClaim(
                "CLM-LK-9021",
                customer,
                app1,
                new BigDecimal("85000.00"),
                new BigDecimal("85000.00"),
                "CASHLESS",
                "APPROVED",
                "Asiri Central Hospital, Colombo 10",
                "Inpatient treatment for Dengue Hemorrhagic Fever with IV fluid therapy and daily platelet monitoring.",
                "asiri_discharge_summary.pdf"
        );

        Claim claim2 = seedClaim(
                "CLM-LK-9022",
                nimal,
                app2,
                new BigDecimal("210000.00"),
                null,
                "REIMBURSEMENT",
                "UNDER_REVIEW",
                "Nawaloka Hospital, Colombo 02",
                "Emergency laparoscopic appendectomy, surgical theater fees, and 3-day recovery room stay.",
                "nawaloka_hospital_itemized_bill.pdf"
        );

        Claim claim3 = seedClaim(
                "CLM-LK-9023",
                dilshan,
                app3,
                new BigDecimal("320000.00"),
                null,
                "CASHLESS",
                "SUBMITTED",
                "Lanka Hospitals, Colombo 05",
                "Cardiology diagnostic coronary angiography and overnight telemetry cardiac monitoring.",
                "lanka_hospitals_preauth_request.pdf"
        );

        // Claim review
        seedClaimReview(claim1, officer, "APPROVED", new BigDecimal("85000.00"),
                "Verified admission invoices and medical chart against Asiri Central Hospital records. Fully eligible under Arogya Gold inpatient entitlement.",
                "None - Verified with Asiri Medical Records Dept");

        // Premium payments
        seedPayment("TXN-LK-20260101-001", customer, app1, new BigDecimal("8500.00"), "CREDIT_CARD", "SUCCESS");
        seedPayment("TXN-LK-20260215-002", nimal, app2, new BigDecimal("3500.00"), "BANK_TRANSFER", "SUCCESS");
        seedPayment("TXN-LK-20260301-003", dilshan, app3, new BigDecimal("14000.00"), "CREDIT_CARD", "SUCCESS");

        // Audit logs
        if (auditLogRepository.count() == 0) {
            seedAuditLog("admin", "SYSTEM_INIT", "Sri Lankan national insurance schema initialized with default parameters", "127.0.0.1");
            seedAuditLog("hospital", "ELIGIBILITY_CHECK", "Checked policy eligibility for patient Kasun Perera at Asiri Central Hospital", "192.168.10.45");
            seedAuditLog("officer", "CLAIM_APPROVED", "Approved cashless settlement of Rs. 85,000.00 for claim CLM-LK-9021", "192.168.10.18");
        }

        log.info("Sri Lankan seed data successfully loaded into database.");
    }

    private User seedUser(String username, String email, String rawPassword, String fullName, String phone, Role role) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername(username);
                    u.setCreatedAt(LocalDateTime.now());
                    return u;
                });
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setPhoneNumber(phone);
        user.setRole(role);
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void cleanUpLegacyUser(String username) {
        userRepository.findByUsername(username).ifPresent(u -> {
            try {
                userRepository.delete(u);
            } catch (Exception ignored) {
            }
        });
    }

    private PolicyPlan seedPlan(String planName, String description, BigDecimal coverage, BigDecimal premium, int durationMonths) {
        return planRepository.findAll().stream()
                .filter(p -> p.getPlanName().equalsIgnoreCase(planName))
                .findFirst()
                .orElseGet(() -> {
                    PolicyPlan plan = new PolicyPlan();
                    plan.setPlanName(planName);
                    plan.setDescription(description);
                    plan.setCoverageAmount(coverage);
                    plan.setMonthlyPremium(premium);
                    plan.setDurationMonths(durationMonths);
                    plan.setActive(true);
                    return planRepository.save(plan);
                });
    }

    private PolicyApplication seedPolicyApplication(String policyNumber, User customer, PolicyPlan plan, LocalDate start, LocalDate end) {
        return applicationRepository.findByPolicyNumber(policyNumber).orElseGet(() -> {
            PolicyApplication app = new PolicyApplication();
            app.setPolicyNumber(policyNumber);
            app.setCustomer(customer);
            app.setPlan(plan);
            app.setStatus("ACTIVE");
            app.setStartDate(start);
            app.setEndDate(end);
            app.setAppliedAt(LocalDateTime.now());
            return applicationRepository.save(app);
        });
    }

    private Claim seedClaim(String claimNumber, User customer, PolicyApplication policy, BigDecimal amount,
                            BigDecimal approvedAmount, String type, String status, String hospital, String desc, String documentName) {
        Claim claim = claimRepository.findByClaimNumber(claimNumber).orElseGet(() -> {
            Claim c = new Claim();
            c.setClaimNumber(claimNumber);
            c.setCustomer(customer);
            c.setPolicy(policy);
            c.setSubmittedAt(LocalDateTime.now().minusDays(5));
            return c;
        });
        claim.setClaimAmount(amount);
        claim.setApprovedAmount(approvedAmount);
        claim.setClaimType(type);
        claim.setStatus(status);
        claim.setHospitalName(hospital);
        claim.setDescription(desc);
        if ("APPROVED".equals(status)) {
            claim.setResolvedAt(LocalDateTime.now().minusDays(1));
        }
        Claim saved = claimRepository.save(claim);

        if (documentName != null && claimDocumentRepository.findByClaimId(saved.getId()).isEmpty()) {
            ClaimDocument doc = new ClaimDocument();
            doc.setClaim(saved);
            doc.setFileName(documentName);
            doc.setFileType("application/pdf");
            doc.setStoragePath("/uploads/claims/" + documentName);
            doc.setUploadedAt(LocalDateTime.now());
            claimDocumentRepository.save(doc);
        }
        return saved;
    }

    private void seedClaimReview(Claim claim, User reviewer, String decision, BigDecimal approvedAmount, String notes, String fraud) {
        if (claimReviewRepository.findByClaimIdOrderByReviewedAtDesc(claim.getId()).isEmpty()) {
            ClaimReview review = new ClaimReview();
            review.setClaim(claim);
            review.setReviewer(reviewer);
            review.setDecision(decision);
            review.setApprovedAmount(approvedAmount);
            review.setReviewerNotes(notes);
            review.setFraudIndicators(fraud);
            review.setReviewedAt(LocalDateTime.now());
            claimReviewRepository.save(review);
        }
    }

    private void seedPayment(String ref, User customer, PolicyApplication policy, BigDecimal amount, String method, String status) {
        if (paymentRepository.findByTransactionReference(ref).isEmpty()) {
            Payment payment = new Payment();
            payment.setTransactionReference(ref);
            payment.setCustomer(customer);
            payment.setPolicy(policy);
            payment.setAmount(amount);
            payment.setPaymentMethod(method);
            payment.setStatus(status);
            payment.setPaidAt(LocalDateTime.now().minusDays(15));
            paymentRepository.save(payment);
        }
    }

    private void seedAuditLog(String username, String action, String details, String ip) {
        AuditLog logEntry = new AuditLog();
        logEntry.setUsername(username);
        logEntry.setAction(action);
        logEntry.setDetails(details);
        logEntry.setIpAddress(ip);
        logEntry.setTimestamp(LocalDateTime.now().minusHours(2));
        auditLogRepository.save(logEntry);
    }
}
