package com.healthinsurance.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlanDto {
    private Long id;
    private String planName;
    private String description;
    private BigDecimal coverageAmount;
    private BigDecimal monthlyPremium;
    private Integer durationMonths;
    private boolean active;
}
