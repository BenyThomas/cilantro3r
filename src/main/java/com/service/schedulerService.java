/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.config.SYSENV;
import com.helper.DateUtil;
import com.helper.TransactionUtil;
import com.repository.CreditRepo;
import com.repository.EftRepo;
import com.repository.Recon_M;
import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author melleji.mollel Mar 12, 2021 6:31:13 AM
 */
@Service
public class schedulerService {

    @Autowired
    EftRepo eftRepo;

    @Autowired
    CreditRepo creditRepo;
    @Autowired
    TransferService transferService;

    @Autowired
    SYSENV sysenv;

    @Autowired
    Recon_M reconRepo;
    @Autowired
    ReportService reportService;
    @Autowired
    PaymentService paymentService;

    @Autowired
    TransactionUtil txnUtil;

//    @Scheduled(fixedDelay = 120000)
    @Scheduled(fixedDelay = 30000)
    public void generateOutwardEFTIso20022() {
        //check for holidays
        if (sysenv.ACTIVE_PROFILE.equals("prod") ) {
            eftRepo.generateOutgoingIso20022();
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processEFTInwardIso20022() {
        //check for holidays
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            eftRepo.processEFTInwardToCorebankingIso20022();
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processLoanRepayment() {
        //check for holidays
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            eftRepo.processLoanRepayment();
        }
    }
    @Scheduled(fixedDelay = 5000)
    public void processTanescoSaccosTransactions() {
        //check for holidays
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            eftRepo.processTanescoSaccosInwardEft();
        }
    }

    @Scheduled(fixedDelay = 60000 * 5)
    public void task1() {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {

            transferService.runUpdateSwiftInfo();
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void task99() {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {

            transferService.sendFileSwift();
        }
    }

    @Scheduled(fixedDelay = 21600000)
    public void task2() throws ParseException, ParseException {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {

            transferService.runTriggerAlert();
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void task5() {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {

            transferService.runDownloadIBFiles();
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void task6() throws InterruptedException {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            reconRepo.mnoArchive(-400);
            reconRepo.cbsArchive(-400);
        }
    }

    @Scheduled(fixedDelay = 60 * 60000 * 3, initialDelay = 10 * 60000)
    public void task7() throws InterruptedException {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            String balanceDate = DateUtil.yesterdayDefault();
           // reportService.sysncCBSBalance(balanceDate);
            reportService.sysncMNOBalance(balanceDate);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void processEFTReturns() {
        //check for holidays
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            eftRepo.processEFTReturnsFromCreditGLToOpLedger();
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void retryFailedCBSPayments() {
        //retry failed payments stuck in queue in PHL_TXN_LOG table
        if (sysenv.ACTIVE_PROFILE.equals("uat")) {
            paymentService.retryFailedCBSPayments();
        }
    }
    @Scheduled(fixedDelay = 1200000)
    public void downloadAirtelVikoba() {
        //retry failed payments stuck in queue in PHL_TXN_LOG table
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            reconRepo.insertVikobaTIPSB2CTxns();
        }
    }


    @Scheduled(cron = "0 0 0 24-29 * *")
    public void DownloadPensionaToPensionPayroll(){
        /*
         * For downloading pensions transactions from transfers to pension_payroll
        */
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            try{
                Thread.sleep(10 * 60000);
                reconRepo.downloadFromTransfersToPensionerPayroll();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

//    1 hour
    @Scheduled(fixedDelay = 3600000)
    public void ProcessEFTandRTGSTransactions(){
        if ( sysenv.ACTIVE_PROFILE.equals("prod")){
            txnUtil.processEFTandRTGSTransactions();
        }
    }
}
