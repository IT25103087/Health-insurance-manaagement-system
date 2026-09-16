package com.healthinsurance.claimsreview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EscalationDto {

    @NotNull(message = "Claim ID is required")
    private Long claimId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String fraudIndicators;
    private String priority; // NORMAL, HIGH, CRITICAL
}
