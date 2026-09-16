package com.healthinsurance.claimsreview.state;

public interface ClaimState {

    String getStatusName();

    boolean canApprove();

    boolean canReject();

    boolean canEscalate();

    boolean canFlagFraud();
}
