package com.service.ubx;

import com.DTO.ubx.CardRegistrationStep;
import com.DTO.ubx.UbxResponse;
import com.config.SYSENV;
import com.models.ubx.CardDetailsEntity;
import com.repository.ubx.CardDetailsRepository;
import com.security.SensitiveDataUtil;
import com.service.BCXService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

// Service layer
@Service
@RequiredArgsConstructor
public class InstantCardService {
    private final CardDetailsRepository cardRepo;
    private final BCXService bcxService;
    private final SYSENV sysEnv;

    @Transactional(transactionManager = "txManagerMaster")
    public UbxResponse registerToCbs(String pan) {
        CardDetailsEntity card = cardRepo.findByPan(pan)
                .orElseThrow(() -> new NoSuchElementException("Card not found"));
        String planOTAC = SensitiveDataUtil.decrypt(card.getNewPin(), sysEnv.SENSITIVE_DATA_ENCRYPTION_KEY);
        UbxResponse cbsRes = bcxService.issueCardToCBS(card,planOTAC);

        String status = ("0".equalsIgnoreCase(cbsRes.getResponseCode()) || "00".equalsIgnoreCase(cbsRes.getResponseCode()))
                ? "SUCCESS" : "FAILED";
        bcxService.recordStep(card, CardRegistrationStep.R, status, cbsRes.getResponseMessage());

        if ("SUCCESS".equals(status)) {
            card.setSavedToCbs(cbsRes.getResponseMessage());
            card.setStatus("R");
            card.setStage("3");
            cardRepo.save(card);
        }
        return cbsRes;
    }
}
