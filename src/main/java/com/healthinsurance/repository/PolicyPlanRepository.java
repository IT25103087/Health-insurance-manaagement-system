package com.healthinsurance.policy.repository;

import com.healthinsurance.policy.entity.PolicyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyPlanRepository extends JpaRepository<PolicyPlan, Long> {

    List<PolicyPlan> findByActiveTrue();
}
