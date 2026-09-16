package com.healthinsurance.policy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyApplicationDto {
    private Long id;
    private String policyNumber;

    @NotNull(message = "Plan ID is required")
    private Long planId;
    private String planName;

    private Long customerId;
    private String customerUsername;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime appliedAt;
}
