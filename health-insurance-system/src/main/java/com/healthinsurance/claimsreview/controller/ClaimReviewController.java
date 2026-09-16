package com.healthinsurance.claimsreview.controller;

import com.healthinsurance.claims.dto.ClaimStatusDto;
import com.healthinsurance.claimsreview.dto.EscalationDto;
import com.healthinsurance.claimsreview.dto.ReviewDecisionDto;
import com.healthinsurance.claimsreview.dto.ReviewRecordDto;
import com.healthinsurance.claimsreview.dto.UpdateReviewNoteDto;
import com.healthinsurance.claimsreview.service.ClaimReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims-review")
public class ClaimReviewController {

    private final ClaimReviewService claimReviewService;

    public ClaimReviewController(ClaimReviewService claimReviewService) {
        this.claimReviewService = claimReviewService;
    }

    // Read
    @GetMapping("/pending")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<ClaimStatusDto>> getPendingReviews() {
        return ResponseEntity.ok(claimReviewService.getPendingClaimsForReview());
    }

    // Read (past review records for one claim - lets the evaluate page show
    // a history list, which is what the PUT/DELETE below act on)
    @GetMapping("/history/{claimId}")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<ReviewRecordDto>> getReviewHistory(@PathVariable Long claimId) {
        return ResponseEntity.ok(claimReviewService.getReviewHistory(claimId));
    }

    // Create (inserts a new ClaimReview record) + Update (claim status)
    @PostMapping("/approve")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ClaimStatusDto> approveClaim(
            @Valid @RequestBody ReviewDecisionDto decisionDto,
            Authentication authentication) {
        return ResponseEntity.ok(claimReviewService.approveClaim(authentication.getName(), decisionDto));
    }

    // Create (inserts a new ClaimReview record) + Update (claim status)
    @PostMapping("/reject")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ClaimStatusDto> rejectClaim(
            @Valid @RequestBody ReviewDecisionDto decisionDto,
            Authentication authentication) {
        return ResponseEntity.ok(claimReviewService.rejectClaim(authentication.getName(), decisionDto));
    }

    // Create (inserts a new ClaimReview record) + Update (claim status)
    @PostMapping("/escalate")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ClaimStatusDto> escalateClaim(
            @Valid @RequestBody EscalationDto escalationDto,
            Authentication authentication) {
        return ResponseEntity.ok(claimReviewService.escalateClaim(authentication.getName(), escalationDto));
    }

    // Create (inserts a new ClaimReview record) + Update (claim status)
    @PostMapping("/flag-fraud")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ClaimStatusDto> flagFraud(
            @Valid @RequestBody EscalationDto escalationDto,
            Authentication authentication) {
        return ResponseEntity.ok(claimReviewService.flagForFraud(authentication.getName(), escalationDto));
    }

    // Update
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ClaimStatusDto> updateReviewNotes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewNoteDto updateDto,
            Authentication authentication) {
        return ResponseEntity.ok(claimReviewService.updateReviewNotes(id, authentication.getName(), updateDto.getReviewerNotes()));
    }

    // Delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id, Authentication authentication) {
        claimReviewService.deleteReview(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
