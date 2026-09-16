package com.healthinsurance.claimsreview.service.impl;

import com.healthinsurance.auth.entity.Role;
import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.claims.dto.ClaimStatusDto;
import com.healthinsurance.claims.entity.Claim;
import com.healthinsurance.claims.entity.ClaimDocument;
import com.healthinsurance.claims.repository.ClaimDocumentRepository;
import com.healthinsurance.claims.repository.ClaimRepository;
import com.healthinsurance.claimsreview.dto.EscalationDto;
import com.healthinsurance.claimsreview.dto.ReviewDecisionDto;
import com.healthinsurance.claimsreview.dto.ReviewRecordDto;
import com.healthinsurance.claimsreview.entity.ClaimReview;
import com.healthinsurance.claimsreview.repository.ClaimReviewRepository;
import com.healthinsurance.claimsreview.service.ClaimReviewService;
import com.healthinsurance.claimsreview.state.ClaimState;
import com.healthinsurance.claimsreview.state.ClaimStateFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClaimReviewServiceImpl implements ClaimReviewService {

    public static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("250000.00");

    private final ClaimReviewRepository claimReviewRepository;
    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ClaimStateFactory stateFactory;

    public ClaimReviewServiceImpl(
            ClaimReviewRepository claimReviewRepository,
            ClaimRepository claimRepository,
            ClaimDocumentRepository documentRepository,
            UserRepository userRepository,
            ClaimStateFactory stateFactory) {
        this.claimReviewRepository = claimReviewRepository;
        this.claimRepository = claimRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.stateFactory = stateFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimStatusDto> getPendingClaimsForReview() {
        List<Claim> all = claimRepository.findAll();
        List<ClaimStatusDto> pending = new ArrayList<>();
        for (Claim c : all) {
            if ("SUBMITTED".equals(c.getStatus()) || "UNDER_REVIEW".equals(c.getStatus()) || "ESCALATED".equals(c.getStatus())) {
                pending.add(mapClaimToDto(c));
            }
        }
        return pending;
    }

    @Override
    @Transactional
    public ClaimStatusDto approveClaim(String reviewerUsername, ReviewDecisionDto decisionDto) {
        Claim claim = claimRepository.findById(decisionDto.getClaimId())
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + decisionDto.getClaimId()));

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerUsername));

        ClaimState state = stateFactory.getState(claim.getStatus());
        if (!state.canApprove()) {
            throw new IllegalStateException("Cannot approve claim currently in status: " + claim.getStatus());
        }

        BigDecimal approved = decisionDto.getApprovedAmount() != null ? decisionDto.getApprovedAmount() : claim.getClaimAmount();
        if (approved == null || approved.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Approved amount must be greater than zero.");
        }
        if (approved.compareTo(claim.getClaimAmount()) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed the submitted claim amount.");
        }

        boolean highValue = claim.getClaimAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0;
        if (highValue && !"ESCALATED".equalsIgnoreCase(claim.getStatus())) {
            throw new IllegalStateException("Claims above Rs. 250,000 must be escalated before approval.");
        }
        if ("ESCALATED".equalsIgnoreCase(claim.getStatus())
                && reviewer.getRole() != Role.OPERATIONS_MANAGER
                && reviewer.getRole() != Role.SYSTEM_ADMIN) {
            throw new IllegalStateException("Only an Operations Manager can approve an escalated claim.");
        }

        claim.setApprovedAmount(approved);
        claim.setStatus("APPROVED");
        claim.setResolvedAt(LocalDateTime.now());
        Claim savedClaim = claimRepository.save(claim);

        ClaimReview review = new ClaimReview();
        review.setClaim(savedClaim);
        review.setReviewer(reviewer);
        review.setDecision("APPROVED");
        review.setApprovedAmount(approved);
        review.setReviewerNotes(decisionDto.getReviewerNotes());
        claimReviewRepository.save(review);

        return mapClaimToDto(savedClaim);
    }

    @Override
    @Transactional
    public ClaimStatusDto rejectClaim(String reviewerUsername, ReviewDecisionDto decisionDto) {
        Claim claim = claimRepository.findById(decisionDto.getClaimId())
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + decisionDto.getClaimId()));

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerUsername));

        ClaimState state = stateFactory.getState(claim.getStatus());
        if (!state.canReject()) {
            throw new IllegalStateException("Cannot reject claim in status: " + claim.getStatus());
        }
        if (decisionDto.getReviewerNotes() == null || decisionDto.getReviewerNotes().isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        claim.setStatus("REJECTED");
        claim.setResolvedAt(LocalDateTime.now());
        Claim savedClaim = claimRepository.save(claim);

        ClaimReview review = new ClaimReview();
        review.setClaim(savedClaim);
        review.setReviewer(reviewer);
        review.setDecision("REJECTED");
        review.setReviewerNotes(decisionDto.getReviewerNotes());
        claimReviewRepository.save(review);

        return mapClaimToDto(savedClaim);
    }

    @Override
    @Transactional
    public ClaimStatusDto escalateClaim(String reviewerUsername, EscalationDto escalationDto) {
        Claim claim = claimRepository.findById(escalationDto.getClaimId())
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + escalationDto.getClaimId()));

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerUsername));

        ClaimState state = stateFactory.getState(claim.getStatus());
        if (!state.canEscalate()) {
            throw new IllegalStateException("Cannot escalate claim in status: " + claim.getStatus());
        }
        if (escalationDto.getReason() == null || escalationDto.getReason().isBlank()) {
            throw new IllegalArgumentException("An escalation reason is required.");
        }

        claim.setStatus("ESCALATED");
        Claim savedClaim = claimRepository.save(claim);

        ClaimReview review = new ClaimReview();
        review.setClaim(savedClaim);
        review.setReviewer(reviewer);
        review.setDecision("ESCALATED");
        review.setReviewerNotes(escalationDto.getReason());
        claimReviewRepository.save(review);

        return mapClaimToDto(savedClaim);
    }

    @Override
    @Transactional
    public ClaimStatusDto flagForFraud(String reviewerUsername, EscalationDto escalationDto) {
        Claim claim = claimRepository.findById(escalationDto.getClaimId())
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + escalationDto.getClaimId()));

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerUsername));

        ClaimState state = stateFactory.getState(claim.getStatus());
        if (!state.canFlagFraud()) {
            throw new IllegalStateException("Cannot flag claim for fraud in status: " + claim.getStatus());
        }
        if (escalationDto.getReason() == null || escalationDto.getReason().isBlank()) {
            throw new IllegalArgumentException("Investigation notes are required.");
        }
        if (escalationDto.getFraudIndicators() == null || escalationDto.getFraudIndicators().isBlank()) {
            throw new IllegalArgumentException("Fraud indicators are required.");
        }

        claim.setStatus("FRAUD_FLAGGED");
        Claim savedClaim = claimRepository.save(claim);

        ClaimReview review = new ClaimReview();
        review.setClaim(savedClaim);
        review.setReviewer(reviewer);
        review.setDecision("FLAGGED_FOR_FRAUD");
        review.setReviewerNotes(escalationDto.getReason());
        review.setFraudIndicators(escalationDto.getFraudIndicators());
        claimReviewRepository.save(review);

        return mapClaimToDto(savedClaim);
    }

    @Override
    @Transactional
    public ClaimStatusDto updateReviewNotes(Long reviewId, String editorUsername, String updatedNotes) {
        ClaimReview review = claimReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review record not found: " + reviewId));

        User editor = userRepository.findByUsername(editorUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + editorUsername));

        boolean isOwner = review.getReviewer().getUsername().equals(editorUsername);
        if (!isOwner && editor.getRole() != Role.OPERATIONS_MANAGER && editor.getRole() != Role.SYSTEM_ADMIN) {
            throw new IllegalArgumentException("Only the original reviewer, an Operations Manager, or an Admin may edit this review note.");
        }
        if (updatedNotes == null || updatedNotes.isBlank()) {
            throw new IllegalArgumentException("Updated notes cannot be empty.");
        }

        review.setReviewerNotes(updatedNotes);
        claimReviewRepository.save(review);

        return mapClaimToDto(review.getClaim());
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, String adminUsername) {
        ClaimReview review = claimReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review record not found: " + reviewId));

        // APPROVED / REJECTED decisions are final financial/audit records and
        // must never be deletable. Only an in-flight ESCALATED or
        // FLAGGED_FOR_FRAUD review can be retracted (e.g. raised in error).
        if ("APPROVED".equals(review.getDecision()) || "REJECTED".equals(review.getDecision())) {
            throw new IllegalStateException("A finalized APPROVED or REJECTED decision is a permanent audit record and cannot be deleted.");
        }

        Claim claim = review.getClaim();
        claim.setStatus("UNDER_REVIEW");
        claimRepository.save(claim);

        claimReviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewRecordDto> getReviewHistory(Long claimId) {
        List<ClaimReview> reviews = claimReviewRepository.findByClaimIdOrderByReviewedAtDesc(claimId);
        List<ReviewRecordDto> result = new ArrayList<>();
        for (ClaimReview r : reviews) {
            result.add(new ReviewRecordDto(
                    r.getId(),
                    r.getClaim().getId(),
                    r.getReviewer().getUsername(),
                    r.getDecision(),
                    r.getApprovedAmount(),
                    r.getReviewerNotes(),
                    r.getFraudIndicators(),
                    r.getReviewedAt()
            ));
        }
        return result;
    }

    private ClaimStatusDto mapClaimToDto(Claim c) {
        List<String> docNames = new ArrayList<>();
        List<ClaimDocument> docs = documentRepository.findByClaimId(c.getId());
        for (ClaimDocument d : docs) {
            docNames.add(d.getFileName());
        }

        return new ClaimStatusDto(
                c.getId(),
                c.getClaimNumber(),
                c.getPolicy().getPolicyNumber(),
                c.getClaimAmount(),
                c.getApprovedAmount(),
                c.getClaimType(),
                c.getStatus(),
                c.getHospitalName(),
                c.getDescription(),
                c.getSubmittedAt(),
                c.getResolvedAt(),
                docNames
        );
    }
}
