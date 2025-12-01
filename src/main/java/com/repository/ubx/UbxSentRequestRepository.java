package com.repository.ubx;

import com.models.ubx.UbxSendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UbxSentRequestRepository extends JpaRepository<UbxSendRequest, Long> {
}
