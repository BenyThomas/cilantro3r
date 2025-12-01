package com.repository.ubx;

import com.models.ubx.CardDetailsEntity;
import com.models.ubx.UBXStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardDetailsRepository extends JpaRepository<CardDetailsEntity, Long>, JpaSpecificationExecutor<CardDetailsEntity> {
    Optional<CardDetailsEntity> findByPan(String pan);
    Optional<CardDetailsEntity> findByReference(String reference);
    Optional<CardDetailsEntity> findByPanAndUbxStatusIn(String pan, List<UBXStatus> ubxStatus);
    @Query(value = "SELECT exists(SELECT 1 FROM card c WHERE c.account_no = :accountNo AND c.status = 'C' AND c.create_dt >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))", nativeQuery = true)
    Integer existsByAccountNumberCreatedWithin30Days(@Param("accountNo") String accountNo);

    @Query("SELECT c FROM CardDetailsEntity c WHERE c.accountNo = :accountNo AND c.status = 'C'")
    List<CardDetailsEntity> findRecentByAccountNumber(@Param("accountNo") String accountNo);


}
