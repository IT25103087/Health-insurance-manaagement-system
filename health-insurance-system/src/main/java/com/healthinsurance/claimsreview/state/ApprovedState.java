package com.healthinsurance.claimsreview.state;

import org.springframework.stereotype.Component;

@Component
public class ApprovedState implements ClaimState {

    @Override
    public String getStatusName() {
        return "APPROVED";
    }

    @Override
    public boolean canApprove() {
        return false; // Already approved
    }

    @Override
    public boolean canReject() {
        return false;
    }

    @Override
    public boolean canEscalate() {
        return false;
    }

    @Override
    public boolean canFlagFraud() {
        return true; // Can still be flagged if audit discovers fraud
    }
}
