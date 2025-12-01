package com.repository;

import com.models.Transfers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.Optional;

@Transactional
@Repository
public interface TransfersRepository extends JpaRepository<Transfers, Long> {
    Optional<Transfers> findByCode(String code);

    Optional<Transfers> findByTxid(String txid);

    Optional<Transfers> findByReference(String reference);

    boolean existsByReference(String reference);

    boolean existsByTxid(String txid);
}
