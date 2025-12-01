package com.service;

import com.config.SYSENV;
import com.queue.QueueProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {
    @Autowired
    SYSENV sysenv;

    @Autowired
    QueueProducer queProducer;

    @Scheduled(fixedRate = 9000000)
//    @Scheduled(cron = "0 0 1 * * *")
    public void downloadGeneralReconDataToReconTracker() {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            //get reconciliation configurations
            queProducer.downloadGeneralReconDataToReconTracker();
        }
    }

//    @Scheduled(fixedRate = 3600000)
    @Scheduled(cron = "0 0 8-9 * * *")
    public void resolveReconException() {
        if (sysenv.ACTIVE_PROFILE.equals("prod")) {
            queProducer.resolveReconException();
        }
    }

    @Scheduled(fixedRate = 4000000)
    public void inQueueSolver(){
        if(sysenv.ACTIVE_PROFILE.equals("prod")){
            queProducer.checkQueuedTxnsInPhlQueue();
        }
    }

////    @Scheduled(fixedRate = 21600000) //six hrs
//    @Scheduled(fixedRate = 21600000)
//    public void processAwaitingCallbackToMoveFromBranchEftGLBOTGL(){
//        if(sysenv.ACTIVE_PROFILE.equals("prod")){
//            queProducer.sendToQueueGabbageCollectorAwaitingCallBack();
//        }
//    }

//    @Scheduled(fixedRate = 51600000)
//    @Scheduled(fixedRate = 6000000) //5mins
//    public void downloadAirtelVikobaToCbsTransactionsTable(){
//        if(sysenv.ACTIVE_PROFILE.equals("prod")){
//            queProducer.downloadAirtelVikobaToCbsTransactionsTable();
//        }
//    }

    @Scheduled(fixedRate = 60000)
    public void downloadDataEnhance(){
        if(sysenv.ACTIVE_PROFILE.equals("kajilo2")){
            queProducer.downloadDataEnhanceFromBothCilantroAndBrinjalToChannelManager();
        }
    }
}
