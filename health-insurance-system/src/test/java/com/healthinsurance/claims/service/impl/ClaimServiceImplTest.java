package com.healthinsurance.claims.service.impl;

import com.healthinsurance.auth.entity.Role;
import com.healthinsurance.auth.entity.User;
import com.healthinsurance.auth.repository.UserRepository;
import com.healthinsurance.claims.dto.ClaimStatusDto;
import com.healthinsurance.claims.dto.ClaimSubmissionDto;
import com.healthinsurance.claims.entity.Claim;
import com.healthinsurance.claims.repository.ClaimDocumentRepository;
import com.healthinsurance.claims.repository.ClaimRepository;
import com.healthinsurance.policy.entity.PolicyApplication;
import com.healthinsurance.policy.repository.PolicyApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimDocumentRepository documentRepository;

    @Mock
    private PolicyApplicationRepository policyRepository;

    @Mock
    private UserRepository userRepository;

    private ClaimServiceImpl claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimServiceImpl(
                claimRepository,
                documentRepository,
                policyRepository,
                userRepository
        );
    }

    @Test
    void customerCannotSubmitClaimForAnotherCustomersPolicy() {
        User requester = customer("kasun");
        User policyOwner = customer("nimal");
        PolicyApplication policy = policy(11L, policyOwner);
        ClaimSubmissionDto submission = submission(11L);

        when(userRepository.findByUsername("kasun")).thenReturn(Optional.of(requester));
        when(policyRepository.findById(11L)).thenReturn(Optional.of(policy));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> claimService.submitClaim("kasun", submission, List.of())
        );

        assertEquals("You are not authorized to submit a claim for this policy.", error.getMessage());
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    void customerCanSubmitClaimForOwnPolicy() {
        User requester = customer("kasun");
        PolicyApplication policy = policy(11L, requester);
        ClaimSubmissionDto submission = submission(11L);

        when(userRepository.findByUsername("kasun")).thenReturn(Optional.of(requester));
        when(policyRepository.findById(11L)).thenReturn(Optional.of(policy));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> {
            Claim saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });
        when(documentRepository.findByClaimId(21L)).thenReturn(List.of());

        ClaimStatusDto result = claimService.submitClaim("kasun", submission, List.of());

        assertEquals(21L, result.getId());
        assertEquals("POL-001", result.getPolicyNumber());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    void customerCannotViewAnotherCustomersClaim() {
        User requester = customer("kasun");
        Claim claim = claim(31L, customer("nimal"));

        when(claimRepository.findById(31L)).thenReturn(Optional.of(claim));
        when(userRepository.findByUsername("kasun")).thenReturn(Optional.of(requester));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> claimService.getClaimStatus(31L, "kasun")
        );

        assertEquals("You are not authorized to view this claim.", error.getMessage());
        verify(documentRepository, never()).findByClaimId(31L);
    }

    @Test
    void hospitalOfficerCanViewCustomerClaim() {
        User hospitalOfficer = new User();
        hospitalOfficer.setUsername("hospital");
        hospitalOfficer.setRole(Role.HOSPITAL_OFFICER);
        Claim claim = claim(31L, customer("nimal"));

        when(claimRepository.findById(31L)).thenReturn(Optional.of(claim));
        when(userRepository.findByUsername("hospital")).thenReturn(Optional.of(hospitalOfficer));
        when(documentRepository.findByClaimId(31L)).thenReturn(List.of());

        ClaimStatusDto result = claimService.getClaimStatus(31L, "hospital");

        assertEquals(31L, result.getId());
        assertEquals("POL-001", result.getPolicyNumber());
    }

    private User customer(String username) {
        User user = new User();
        user.setUsername(username);
        user.setRole(Role.CUSTOMER);
        return user;
    }

    private PolicyApplication policy(Long id, User customer) {
        PolicyApplication policy = new PolicyApplication();
        policy.setId(id);
        policy.setPolicyNumber("POL-001");
        policy.setCustomer(customer);
        policy.setStatus("ACTIVE");
        return policy;
    }

    private ClaimSubmissionDto submission(Long policyId) {
        ClaimSubmissionDto submission = new ClaimSubmissionDto();
        submission.setPolicyId(policyId);
        submission.setClaimAmount(new BigDecimal("5000.00"));
        submission.setHospitalName("Test Hospital");
        submission.setDescription("Test treatment");
        return submission;
    }

    private Claim claim(Long id, User owner) {
        Claim claim = new Claim();
        claim.setId(id);
        claim.setClaimNumber("CLM-001");
        claim.setCustomer(owner);
        claim.setPolicy(policy(11L, owner));
        claim.setClaimAmount(new BigDecimal("5000.00"));
        claim.setApprovedAmount(BigDecimal.ZERO);
        claim.setClaimType("REIMBURSEMENT");
        claim.setStatus("SUBMITTED");
        return claim;
    }
}
