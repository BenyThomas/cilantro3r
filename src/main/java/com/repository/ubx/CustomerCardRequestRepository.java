package com.repository.ubx;

import com.models.ubx.CustomerCardRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerCardRequestRepository extends JpaRepository<CustomerCardRequest, Integer> {
    Optional<CustomerCardRequest> findByCardNumberAndAccountNumber(String pan, String accountNo);
}
