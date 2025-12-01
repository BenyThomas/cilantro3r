package com.repository;

import com.models.AllowedSTPAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.Optional;

@Repository
public interface AllowedSTPAccountRepository extends JpaRepository<AllowedSTPAccount, Long> {
    Optional<AllowedSTPAccount> findByAcctNo(String acctNo);
    boolean existsAllowedSTPAccountsByAcctNo(String acctNo);
}
