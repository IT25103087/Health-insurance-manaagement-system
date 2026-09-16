package com.healthinsurance.policy.repository;

import com.healthinsurance.policy.entity.PolicyApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyApplicationRepository extends JpaRepository<PolicyApplication, Long> {

    List<PolicyApplication> findByCustomerUsername(String username);

    Optional<PolicyApplication> findByPolicyNumber(String policyNumber);

    List<PolicyApplication> findByStatus(String status);
}
