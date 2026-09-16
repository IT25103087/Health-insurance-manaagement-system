package com.healthinsurance.claimsreview.controller;

import com.healthinsurance.claims.dto.ClaimStatusDto;
import com.healthinsurance.claims.service.ClaimService;
import com.healthinsurance.claimsreview.service.ClaimReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ClaimReviewWebController {

    private final ClaimReviewService claimReviewService;
    private final ClaimService claimService;

    public ClaimReviewWebController(ClaimReviewService claimReviewService, ClaimService claimService) {
        this.claimReviewService = claimReviewService;
        this.claimService = claimService;
    }

    @GetMapping("/claims-review/queue")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public String reviewQueue(Model model) {
        model.addAttribute("pendingClaims", claimReviewService.getPendingClaimsForReview());
        return "claimsreview/review-queue";
    }

    @GetMapping("/claims-review/evaluate/{id}")
    @PreAuthorize("hasRole('CLAIMS_OFFICER') or hasRole('OPERATIONS_MANAGER') or hasRole('SYSTEM_ADMIN')")
    public String evaluateClaim(@PathVariable("id") Long id, Model model) {
        try {
            ClaimStatusDto claim = claimService.getClaimStatus(id, null);
            model.addAttribute("claim", claim);
            return "claimsreview/evaluate";
        } catch (IllegalArgumentException e) {
            return "redirect:/claims-review/queue";
        }
    }
}
