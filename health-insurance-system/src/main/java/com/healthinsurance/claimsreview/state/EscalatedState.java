package com.healthinsurance.claimsreview.state;

import org.springframework.stereotype.Component;

@Component
public class EscalatedState implements ClaimState {

    @Override
    public String getStatusName() {
        return "ESCALATED";
    }

    @Override
    public boolean canApprove() {
        return true; // Operations Manager can approve escalated claim
    }

    @Override
    public boolean canReject() {
        return true;
    }

    @Override
    public boolean canEscalate() {
        return false; // Already escalated
    }

    @Override
    public boolean canFlagFraud() {
        return true;
    }
}
