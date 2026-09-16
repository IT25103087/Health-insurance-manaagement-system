package com.healthinsurance.claimsreview.state;

import org.springframework.stereotype.Component;

@Component
public class UnderReviewState implements ClaimState {

    @Override
    public String getStatusName() {
        return "UNDER_REVIEW";
    }

    @Override
    public boolean canApprove() {
        return true;
    }

    @Override
    public boolean canReject() {
        return true;
    }

    @Override
    public boolean canEscalate() {
        return true;
    }

    @Override
    public boolean canFlagFraud() {
        return true;
    }
}
