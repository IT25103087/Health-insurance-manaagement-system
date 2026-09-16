package com.healthinsurance.claimsreview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecordDto {
    private Long id;
    private Long claimId;
    private String reviewerUsername;
    private String decision;
    private BigDecimal approvedAmount;
    private String reviewerNotes;
    private String fraudIndicators;
    private LocalDateTime reviewedAt;
}
