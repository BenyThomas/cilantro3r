package com.service;

import com.config.SYSENV;
import com.helper.DateUtil;
import com.models.PensionersPayroll;
import com.models.TmpBatchTransaction;
import com.repository.PensionersPayrollRepository;
import com.repository.TmpBatchTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PensionersPayrollService {

    @Autowired
    private PensionersPayrollRepository pensionersPayrollRepository;
    @Autowired
    TmpBatchTransactionRepository tmpBatchTransactionRepository;

    @Autowired
    SYSENV systemVariable;

    public void processPayrollInChunks(String batchReference) {
        int chunkSize = 5000;
        int pageNumber = 0;

        try {
            while (pageNumber < 30 ) {
                List<PensionersPayroll> pensionersPayrollList = pensionersPayrollRepository.findAllByPensionItemForSplit(batchReference);

                if(pensionersPayrollList.size()>0) {
                    // Create a sub-batch reference for this chunk
                    String subBatchReference = batchReference + "_" + DateUtil.now("yyyyMMddHHmmss") + "_" + pageNumber;

                    // Process the chunk (you can implement you logic here)
                    processChunk(pensionersPayrollList, subBatchReference, false);

                }else{
                    break;
                }
                pageNumber++;
            }

        }catch (Exception e){
            throw new  RuntimeException(e.getMessage());
        }
    }

    public void processPayrollInChunksForLoanRepayment(String batchReference) {
        int chunkSize = 5000;
        int pageNumber = 0;

        try {
            while (pageNumber < 40 ) {
                List<PensionersPayroll> pensionersPayrollList = pensionersPayrollRepository.findAllByPensionItemForSplitForLoanRepayment(batchReference);

                if(pensionersPayrollList.size()>0) {
                    // Create a sub-batch reference for this chunk
                    String subBatchReference = batchReference + "_" + DateUtil.now("yyyyMMddHHmmss") + "_od_" + pageNumber;

                    // Process the chunk (you can implement you logic here)
                    processChunk(pensionersPayrollList, subBatchReference, true);

                }else{
                    break;
                }
                pageNumber++;
            }

        }catch (Exception e){
            throw new  RuntimeException(e.getMessage());
        }
    }


    private void processChunk(List<PensionersPayroll> payrollChunk, String subBatchReference,Boolean userOdUrl) {
        // Implement your logic to process each chunk and assign the sub-batch reference
        BigDecimal totalAmount = new BigDecimal("0.0");
        for (PensionersPayroll payroll : payrollChunk) {
            payroll.setSubBatchReference(subBatchReference);
            totalAmount = totalAmount.add(payroll.getAmount());
        }
        pensionersPayrollRepository.saveAllAndFlush(payrollChunk);
        if(payrollChunk.size()>0) {
            //TODO: INSERT INTO tmp_batch_transaction
            TmpBatchTransaction tmpBatchTransaction = new TmpBatchTransaction();
            tmpBatchTransaction.setReference(subBatchReference);
            tmpBatchTransaction.setResult("-1");
            tmpBatchTransaction.setItemCount(payrollChunk.size());
            tmpBatchTransaction.setFailureCount(0l);
            tmpBatchTransaction.setEndRecId(0);
            tmpBatchTransaction.setStartRecId(0);
            tmpBatchTransaction.setFailureCount(0l);
            tmpBatchTransaction.setSuccessCount(0l);
            tmpBatchTransaction.setTotalAmount(totalAmount);
            tmpBatchTransaction.setCallbackUrl(userOdUrl ? systemVariable.LOCAL_CALLBACK_URL + "/api/pension/od/repay" : systemVariable.LOCAL_CALLBACK_URL + "/api/batchCallback");
            tmpBatchTransactionRepository.saveAndFlush(tmpBatchTransaction);
        }
    }


}
