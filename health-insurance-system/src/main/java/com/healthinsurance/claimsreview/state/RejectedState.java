package com.healthinsurance.claimsreview.state;

import org.springframework.stereotype.Component;

@Component
public class RejectedState implements ClaimState {

    @Override
    public String getStatusName() {
        return "REJECTED";
    }

    @Override
    public boolean canApprove() {
        return false;
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
        return true;
    }
}
