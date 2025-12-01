package com.repository;

import com.models.TmpBatchTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TmpBatchTransactionRepository extends JpaRepository<TmpBatchTransaction, String> {

    // Find all transactions by result
    List<TmpBatchTransaction> findByResult(String result);
    Optional<TmpBatchTransaction> findByReference(String reference);

// Find all transactions with total amount greater
}
