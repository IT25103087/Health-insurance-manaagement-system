package com.healthinsurance.policy.service;

import com.healthinsurance.policy.dto.PolicyApplicationDto;
import com.healthinsurance.policy.dto.PolicyPlanDto;

import java.util.List;

public interface PolicyService {

    List<PolicyPlanDto> getAllActivePlans();

    PolicyPlanDto getPlanById(Long planId);

    PolicyApplicationDto applyForPolicy(String username, PolicyApplicationDto applicationDto);

    List<PolicyApplicationDto> getCustomerPolicies(String username);

    byte[] generatePolicyPdf(Long policyId);

    PolicyApplicationDto renewPolicy(Long policyId, String username);

    PolicyApplicationDto cancelPolicy(Long policyId, String username);

    PolicyApplicationDto updatePolicy(Long policyId, PolicyApplicationDto updateDto, String username);

    void deletePolicy(Long policyId, String username);
}
