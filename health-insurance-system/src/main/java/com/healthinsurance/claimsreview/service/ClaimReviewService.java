package com.healthinsurance.claimsreview.service;

import com.healthinsurance.claims.dto.ClaimStatusDto;
import com.healthinsurance.claimsreview.dto.EscalationDto;
import com.healthinsurance.claimsreview.dto.ReviewDecisionDto;
import com.healthinsurance.claimsreview.dto.ReviewRecordDto;

import java.util.List;

public interface ClaimReviewService {

    List<ClaimStatusDto> getPendingClaimsForReview();

    ClaimStatusDto approveClaim(String reviewerUsername, ReviewDecisionDto decisionDto);

    ClaimStatusDto rejectClaim(String reviewerUsername, ReviewDecisionDto decisionDto);

    ClaimStatusDto escalateClaim(String reviewerUsername, EscalationDto escalationDto);

    ClaimStatusDto flagForFraud(String reviewerUsername, EscalationDto escalationDto);

    ClaimStatusDto updateReviewNotes(Long reviewId, String editorUsername, String updatedNotes);

    void deleteReview(Long reviewId, String adminUsername);

    List<ReviewRecordDto> getReviewHistory(Long claimId);
}
