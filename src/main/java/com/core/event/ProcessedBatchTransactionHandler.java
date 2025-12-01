package com.core.event;

import com.repository.PensionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;
import java.util.Objects;

@Component
public class ProcessedBatchTransactionHandler {
    @Autowired
    private PensionRepo pensionRepo;

    @Autowired
    private HttpSession session;

    @EventListener
    public void handleBatchCallbackEvent(final BatchCallbackEvent batchCallbackEvent) {
        try {
            if (Objects.equals(batchCallbackEvent.getResult(), "00")) {
                pensionRepo.updateBatchSuccessCount(batchCallbackEvent.getBatch());
            } else {
                pensionRepo.updateBatchFailureCount(batchCallbackEvent.getBatch());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
