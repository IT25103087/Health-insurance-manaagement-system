package com.healthinsurance.billing.controller;

import com.healthinsurance.billing.service.BillingService;
import com.healthinsurance.policy.dto.PolicyApplicationDto;
import com.healthinsurance.policy.service.PolicyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BillingWebController {

    private final BillingService billingService;
    private final PolicyService policyService;

    public BillingWebController(BillingService billingService, PolicyService policyService) {
        this.billingService = billingService;
        this.policyService = policyService;
    }

    @GetMapping("/billing/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String payPage(@RequestParam(value = "policyId", required = false) Long policyId,
                          Authentication authentication,
                          Model model) {
        List<PolicyApplicationDto> policies = policyService.getCustomerPolicies(authentication.getName());
        model.addAttribute("policies", policies);
        model.addAttribute("selectedPolicyId", policyId);
        return "billing/pay";
    }

    @GetMapping("/billing/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String historyPage(Authentication authentication, Model model) {
        model.addAttribute("payments", billingService.getPaymentHistory(authentication.getName()));
        return "billing/history";
    }

    @GetMapping("/billing/refunds")
    @PreAuthorize("hasRole('OPERATIONS_MANAGER') or hasRole('SUPPORT_REP') or hasRole('SYSTEM_ADMIN')")
    public String refundsPage(Model model) {
        model.addAttribute("refunds", billingService.getAllRefundRequests());
        return "billing/refunds";
    }
}
