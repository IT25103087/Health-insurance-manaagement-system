package com.healthinsurance.policy.controller;

import com.healthinsurance.policy.dto.PolicyApplicationDto;
import com.healthinsurance.policy.dto.PolicyPlanDto;
import com.healthinsurance.policy.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    // Read
    @GetMapping("/plans")
    public ResponseEntity<List<PolicyPlanDto>> listPlans() {
        return ResponseEntity.ok(policyService.getAllActivePlans());
    }

    // Create
    @PostMapping("/apply")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT')")
    public ResponseEntity<PolicyApplicationDto> applyForPolicy(
            @Valid @RequestBody PolicyApplicationDto applicationDto,
            Authentication authentication) {
        PolicyApplicationDto created = policyService.applyForPolicy(authentication.getName(), applicationDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Read
    @GetMapping("/my-policies")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PolicyApplicationDto>> getMyPolicies(Authentication authentication) {
        return ResponseEntity.ok(policyService.getCustomerPolicies(authentication.getName()));
    }

    // Read
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT')")
    public ResponseEntity<byte[]> downloadPolicyPdf(@PathVariable Long id) {
        byte[] pdfBytes = policyService.generatePolicyPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=policy-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Update (status transition)
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT')")
    public ResponseEntity<PolicyApplicationDto> renewPolicy(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(policyService.renewPolicy(id, authentication.getName()));
    }

    // Update (status transition)
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT')")
    public ResponseEntity<PolicyApplicationDto> cancelPolicy(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(policyService.cancelPolicy(id, authentication.getName()));
    }

    // Update
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('INSURANCE_AGENT') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<PolicyApplicationDto> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyApplicationDto updateDto,
            Authentication authentication) {
        return ResponseEntity.ok(policyService.updatePolicy(id, updateDto, authentication.getName()));
    }

    // Delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSURANCE_AGENT') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id, Authentication authentication) {
        policyService.deletePolicy(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
