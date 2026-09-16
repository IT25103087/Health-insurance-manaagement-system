package com.healthinsurance.claimsreview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionDto {

    @NotNull(message = "Claim ID is required")
    private Long claimId;

    @NotBlank(message = "Decision is required")
    private String decision; // APPROVED, REJECTED

    private BigDecimal approvedAmount;
    private String reviewerNotes;
}
