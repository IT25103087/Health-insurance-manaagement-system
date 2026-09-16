package com.healthinsurance.claimsreview.state;

import org.springframework.stereotype.Component;

@Component
public class ClaimStateFactory {

    private final SubmittedState submittedState;
    private final UnderReviewState underReviewState;
    private final ApprovedState approvedState;
    private final RejectedState rejectedState;
    private final EscalatedState escalatedState;

    public ClaimStateFactory(
            SubmittedState submittedState,
            UnderReviewState underReviewState,
            ApprovedState approvedState,
            RejectedState rejectedState,
            EscalatedState escalatedState) {
        this.submittedState = submittedState;
        this.underReviewState = underReviewState;
        this.approvedState = approvedState;
        this.rejectedState = rejectedState;
        this.escalatedState = escalatedState;
    }

    public ClaimState getState(String status) {
        if (status == null) return submittedState;
        switch (status.toUpperCase()) {
            case "UNDER_REVIEW":
                return underReviewState;
            case "APPROVED":
                return approvedState;
            case "REJECTED":
                return rejectedState;
            case "ESCALATED":
                return escalatedState;
            default:
                return submittedState;
        }
    }
}
