package com.healthinsurance.billing.repository;

import com.healthinsurance.billing.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    List<RefundRequest> findByCustomerUsername(String username);

    List<RefundRequest> findByStatus(String status);

    List<RefundRequest> findByPaymentId(Long paymentId);
}
