package com.healthinsurance.claimsreview.state;

import org.springframework.stereotype.Component;

@Component
public class SubmittedState implements ClaimState {

    @Override
    public String getStatusName() {
        return "SUBMITTED";
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
