package com.healthinsurance.claimsreview.repository;

import com.healthinsurance.claimsreview.entity.ClaimReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimReviewRepository extends JpaRepository<ClaimReview, Long> {

    List<ClaimReview> findByClaimIdOrderByReviewedAtDesc(Long claimId);

    List<ClaimReview> findByReviewerUsername(String username);

    List<ClaimReview> findByDecision(String decision);
}
