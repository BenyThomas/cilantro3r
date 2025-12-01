package com.repository;

import com.models.PensionersPayroll;
import com.models.TmpBatchTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.List;

@Transactional
@Repository
public interface PensionersPayrollRepository extends JpaRepository<PensionersPayroll, Long> {
    Page<PensionersPayroll> findByBatchReference(String batch, Pageable pageable);

    @Query(value = "SELECT * FROM pensioners_payroll where od_loan_status='0' and cbs_status<>'F' and cbs_status is not null  and sub_batch_reference is null and batchReference=:batchReference order by id asc limit 5000",
            nativeQuery = true
    )
    List<PensionersPayroll> findAllByPensionItemForSplit(@Param("batchReference")String batchReference);

    @Query(value = "SELECT * FROM pensioners_payroll where od_loan_status='1' and cbs_status<>'F' and cbs_status is not null  and sub_batch_reference is null and batchReference=:batchReference order by id limit 5000",
            nativeQuery = true)
    List<PensionersPayroll> findAllByPensionItemForSplitForLoanRepayment(@Param("batchReference")String batchReference
    );

    List<PensionersPayroll> findBySubBatchReference(String ref);


}
