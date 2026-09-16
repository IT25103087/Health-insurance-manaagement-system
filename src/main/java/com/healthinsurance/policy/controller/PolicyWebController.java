package com.healthinsurance.policy.controller;

import com.healthinsurance.policy.dto.PolicyPlanDto;
import com.healthinsurance.policy.service.PolicyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PolicyWebController {

    private final PolicyService policyService;

    public PolicyWebController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/policies/plans")
    public String viewPlans(Model model) {
        List<PolicyPlanDto> plans = policyService.getAllActivePlans();
        model.addAttribute("plans", plans);
        return "policy/plans";
    }

    @GetMapping("/policies/apply")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT')")
    public String applyPage(@RequestParam("planId") Long planId, Model model) {
        PolicyPlanDto plan = policyService.getPlanById(planId);
        model.addAttribute("plan", plan);
        return "policy/apply";
    }

    @GetMapping("/policies/my")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT')")
    public String myPolicies(Authentication authentication, Model model) {
        model.addAttribute("policies", policyService.getCustomerPolicies(authentication.getName()));
        return "policy/my-policies";
    }
}
