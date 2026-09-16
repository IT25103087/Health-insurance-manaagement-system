package com.healthinsurance.policy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "policy_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String planName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal coverageAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPremium;

    @Column(nullable = false)
    private Integer durationMonths;

    @Column(nullable = false)
    private boolean active = true;
}
