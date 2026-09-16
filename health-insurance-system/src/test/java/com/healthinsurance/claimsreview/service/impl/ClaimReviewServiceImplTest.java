package com.healthinsurance.claimsreview.service.impl;

import com.healthinsurance.auth.entity.Role;
import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.claims.entity.Claim;
import com.healthinsurance.claims.repository.ClaimDocumentRepository;
import com.healthinsurance.claims.repository.ClaimRepository;
import com.healthinsurance.claimsreview.dto.EscalationDto;
import com.healthinsurance.claimsreview.dto.ReviewDecisionDto;
import com.healthinsurance.claimsreview.repository.ClaimReviewRepository;
import com.healthinsurance.claimsreview.state.ClaimState;
import com.healthinsurance.claimsreview.state.ClaimStateFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimReviewServiceImplTest {

    @Mock private ClaimReviewRepository reviewRepository;
    @Mock private ClaimRepository claimRepository;
    @Mock private ClaimDocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClaimStateFactory stateFactory;
    @Mock private ClaimState state;

    private ClaimReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClaimReviewServiceImpl(
                reviewRepository, claimRepository, documentRepository, userRepository, stateFactory
        );
    }

    @Test
    void approvedAmountCannotExceedClaimAmount() {
        Claim claim = claim("SUBMITTED", "100000.00");
        ReviewDecisionDto decision = decision("120000.00", "Reviewed");
        stubReview(claim, officer(Role.CLAIMS_OFFICER));
        when(state.canApprove()).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.approveClaim("officer", decision)
        );

        assertEquals("Approved amount cannot exceed the submitted claim amount.", error.getMessage());
        verify(claimRepository, never()).save(claim);
    }

    @Test
    void highValueClaimMustBeEscalatedBeforeApproval() {
        Claim claim = claim("SUBMITTED", "320000.00");
        ReviewDecisionDto decision = decision("300000.00", "Reviewed");
        stubReview(claim, officer(Role.CLAIMS_OFFICER));
        when(state.canApprove()).thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.approveClaim("officer", decision)
        );

        assertEquals("Claims above Rs. 250,000 must be escalated before approval.", error.getMessage());
    }

    @Test
    void claimsOfficerCannotApproveEscalatedClaim() {
        Claim claim = claim("ESCALATED", "320000.00");
        ReviewDecisionDto decision = decision("300000.00", "Reviewed");
        stubReview(claim, officer(Role.CLAIMS_OFFICER));
        when(state.canApprove()).thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.approveClaim("officer", decision)
        );

        assertEquals("Only an Operations Manager can approve an escalated claim.", error.getMessage());
    }

    @Test
    void rejectionRequiresReason() {
        Claim claim = claim("SUBMITTED", "100000.00");
        ReviewDecisionDto decision = decision(null, "  ");
        stubReview(claim, officer(Role.CLAIMS_OFFICER));
        when(state.canReject()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.rejectClaim("officer", decision));
    }

    @Test
    void fraudFlagRequiresIndicators() {
        Claim claim = claim("SUBMITTED", "100000.00");
        EscalationDto escalation = new EscalationDto(1L, "Suspicious invoice", " ", "HIGH");
        stubReview(claim, officer(Role.CLAIMS_OFFICER));
        when(state.canFlagFraud()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.flagForFraud("officer", escalation));
    }

    private void stubReview(Claim claim, User reviewer) {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(userRepository.findByUsername("officer")).thenReturn(Optional.of(reviewer));
        when(stateFactory.getState(claim.getStatus())).thenReturn(state);
    }

    private User officer(Role role) {
        User user = new User();
        user.setUsername("officer");
        user.setRole(role);
        return user;
    }

    private Claim claim(String status, String amount) {
        Claim claim = new Claim();
        claim.setId(1L);
        claim.setStatus(status);
        claim.setClaimAmount(new BigDecimal(amount));
        return claim;
    }

    private ReviewDecisionDto decision(String amount, String notes) {
        ReviewDecisionDto decision = new ReviewDecisionDto();
        decision.setClaimId(1L);
        decision.setDecision("APPROVED");
        decision.setApprovedAmount(amount == null ? null : new BigDecimal(amount));
        decision.setReviewerNotes(notes);
        return decision;
    }
}
