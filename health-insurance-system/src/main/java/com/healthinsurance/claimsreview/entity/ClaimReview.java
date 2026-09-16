package com.healthinsurance.claimsreview.entity;

import com.healthinsurance.auth.entity.User;
import com.healthinsurance.claims.entity.Claim;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(nullable = false, length = 30)
    private String decision; // APPROVED, REJECTED, ESCALATED, FLAGGED_FOR_FRAUD

    @Column(precision = 10, scale = 2)
    private BigDecimal approvedAmount;

    @Column(length = 500)
    private String reviewerNotes;

    @Column(length = 500)
    private String fraudIndicators;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        this.reviewedAt = LocalDateTime.now();
    }
}
