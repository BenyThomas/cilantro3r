package com.controller.ubx;

import com.DTO.Ebanking.CardRegistrationReq;
import com.DTO.ubx.*;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.APIResponse;
import com.models.ubx.CardActionStatus;
import com.models.ubx.CardDetailsEntity;
import com.repository.ubx.CardActionStatusRepository;
import com.repository.ubx.CardDetailsRepository;
import com.security.SensitiveDataUtil;
import com.service.BCXService;
import com.service.TransferService;
import com.service.ubx.InstantCardService;
import com.service.ubx.UrlEndpointService;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/instant", produces = MediaType.APPLICATION_JSON_VALUE)
public class InstantCardIssueApiController {

    @Autowired private BCXService bcxService;
    @Autowired private CardDetailsRepository cardRepo;
    @Autowired private CardActionStatusRepository actionRepo;
    @Autowired private TransferService transferService;
    @Autowired private SYSENV env;
    @Autowired
    private InstantCardService instantCardService;
    @Autowired
    private CardDetailsRepository cardDetailsRepository;
    @Autowired
    ObjectMapper objectMapper;

    /* ---------------------------
     * 1) LINK CARD (Stage 1)
     * --------------------------- */
    @PostMapping("/card/link")
    public APIResponse<UbxResponse> linkCard(@RequestBody IssueCardLinkRequest req, HttpSession session) {
        Optional<CardDetailsEntity> cardOpt = cardDetailsRepository.findByPan(req.getCardNumber());
        if (!cardOpt.isPresent()) return APIResponse.error("Card not found");

        CardDetailsEntity card = cardOpt.get();
        try {
            final String username = safeUsername();
            final String branch   = safeBranch(session);

            // --- build request payload for BCX ---
            CustomerInfoReq inf = new CustomerInfoReq();

            // Names (safe split)
            String fullName = nvl(card.getCustomerName(), "");
            NameParts np = splitName(fullName, nvl(card.getCustomerShortName(), ""));

            inf.setFirstName(np.first);
            inf.setLastName(np.last);
            inf.setCustomerName(fullName);

            // Identifiers
            String cif = nvl(card.getCustid(), "");                 // entity field is 'custid'
            String rim = nvl(card.getCustomerRirmNo(), "");         // note: entity has 'customerRirmNo' (typo in name)

            inf.setCif(cif);
            inf.setCustId(cif);             // keep both in sync for UBX if they accept either
            inf.setCustomerRim(rim);

            // Card / account
            inf.setCardNumber(nvl(req.getCardNumber(), card.getPan()));
            inf.setAccountNumber(nvl(req.getAccountNumber(), card.getAccountNo()));
            inf.setAccountType("10");    // if you have a rule, set it here

            // Contact
            inf.setMsisdn(nvl(req.getMsisdn(), card.getPhone()));
            inf.setEmail(nvl(req.getEmail(), card.getEmail()));

            // PAN expiry (YYMM or from DB’s `cardexpireDt`)
            inf.setPanExpireDate(nvl(card.getCardexpireDt(),""));

            // Channel / branch / user
            inf.setChannelId(nvl(card.getChannelId(), "")); // prefer card’s recorded channel
            inf.setBranchCode(branch);
            inf.setCurrentUserName(card.getChannelId());

            // Category & charge
            inf.setCategory(nvl(req.getCategory(), card.getCustomerCategory()));
            if (card.getCharge() != null) {
                inf.setCharge(String.valueOf(card.getCharge())); // @JsonIgnore on field; used only internally
            }

            // Optional fields you might have (set if available)
            inf.setNationalId(nvl(card.getNationalId(), ""));
            inf.setNewPin(SensitiveDataUtil.decrypt(card.getNewPin(),env.SENSITIVE_DATA_ENCRYPTION_KEY)); // not part of link step

            // Fire
            UbxResponse res = bcxService.linkCardToAccount(inf);
            return isOk(res) ? APIResponse.ok(res) : APIResponse.error(res.getResponseMessage());

        } catch (Exception e) {
            return APIResponse.error(e.getMessage());
        }
    }

    /* ===== helpers ===== */

    private static String nvl(String v, String fallback) {
        return (v == null || v.trim().isEmpty()) ? fallback : v.trim();
    }

    private static final class NameParts {
        final String first; final String last;
        NameParts(String f, String l) { this.first = f; this.last = l; }
    }

    /**
     * Split a full name into first/last safely.
     * - If only one token, put it in first and leave last blank.
     * - If shortName present, prefer it as first; use remaining as last.
     */
    private static NameParts splitName(String customerName, String shortName) {
        String name = customerName == null ? "" : customerName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            return new NameParts(nvl(shortName, ""), "");
        }
        String[] parts = name.split(" ");
        if (parts.length == 1) {
            return new NameParts(nvl(shortName, parts[0]), "");
        }
        // First name:
        String first = nvl(shortName, parts[0]);
        // Last name = everything after the first token
        String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new NameParts(first, last);
    }

    /* ---------------------------
     * 2) REISSUE PIN (Stage 2)
     * --------------------------- */
    @PostMapping("/card/reissue-pin")
    public APIResponse<UbxResponse> reissuePin(@RequestBody CardNoOnly body, HttpSession session) {
        Optional<CardDetailsEntity> cardOpt = cardDetailsRepository.findByPan(body.getCardNumber());
        if (!cardOpt.isPresent()) return APIResponse.error("Card not found");
        CardDetailsEntity card = cardOpt.get();
        try {
            ReissueCardPinRequest r = new ReissueCardPinRequest();
            r.setCardNumber(card.getPan());
            r.setChannelId(card.getChannelId());
            r.setInstitutionId(env.UBX_CLIENT_ID);
            UbxResponse res = bcxService.reissueCardPin(r);
            return isOk(res) ? APIResponse.ok(res) : APIResponse.error(res.getResponseMessage());
        } catch (Exception e) {
            return APIResponse.error(e.getMessage());
        }
    }

    /* ----------------------------------------------------
     * 3) REGISTER TO CBS (Stage 3) – uses the chosen PIN
     * ---------------------------------------------------- */
    @PostMapping("/card/register-cbs")
    public ResponseEntity<UbxResponse> registerToCBS(@RequestBody RegisterCBSRequest body) {
        try {
            UbxResponse res = instantCardService.registerToCbs(body.getPan());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.ok(new UbxResponse("Unknown Error","96"));
        }
    }
    /* ---------------------------
     * 4) CHARGE (Stage 4)
     * --------------------------- */
    @PostMapping("/card/charge")
    public ResponseEntity<UbxResponse> charge(@RequestBody CardNoOnly body) {
        CardDetailsEntity card = cardRepo.findByPan(body.getCardNumber())
                .orElseThrow(() -> new NoSuchElementException("Card not found"));
        try {
            bcxService.beginStep(card, CardRegistrationStep.A, "Charging Card issuance fee");

            if (card.isCharged()) {
                bcxService.recordStep(card, CardRegistrationStep.A, "SUCCESS", "Already charged");
                card.setStage("4");
                card.setStatus("A");
                cardRepo.save(card);
                return ResponseEntity.ok(new UbxResponse("Already charged", "00"));
            }

            String ref = card.getReference();
            int code = transferService.procChargeForVisaCard(
                    ref,
                    card.getAccountNo(),
                    card.getCollectingBranch(),
                    "TZS"
            );

            if (code == 0) {
                card.setCharged(true);
                card.setStage("4");
                card.setStatus("A");
                cardRepo.save(card);
                bcxService.recordStep(card, CardRegistrationStep.C, "SUCCESS", "Approved or completed successfully");
                return ResponseEntity.ok(new UbxResponse("Charge successful", "00"));
            } else if (code == 51) {
                bcxService.recordStep(card, CardRegistrationStep.C, "FAILED", "Insufficient funds");
                card.setStatus("F");
                cardRepo.save(card);
                return ResponseEntity.ok(new UbxResponse("Insufficient funds","51"));
            } else if (code == 26) {
                bcxService.recordStep(card, CardRegistrationStep.C, "FAILED", "Duplicate transaction");
                card.setStatus("F");
                cardRepo.save(card);
                return ResponseEntity.ok(new UbxResponse("Duplicate transaction","26"));
            } else {
                bcxService.recordStep(card, CardRegistrationStep.C, "FAILED", "General error in core banking");
                card.setStatus("F");
                cardRepo.save(card);
                return ResponseEntity.ok(new UbxResponse("General error in core banking","96"));
            }
        } catch (Exception e) {
            card.setStatus("F");
            cardRepo.save(card);
            return ResponseEntity.ok(new UbxResponse("An Unexpected error occurred", "96"));
        }
    }

    /* ---------------------------
     * 5) CHANGE PIN (Stage 5)
     * --------------------------- */
    @PostMapping("/card/change-pin")
    public APIResponse<UbxResponse> changePin(@RequestBody ChangePinRequest body, HttpSession session) {
        Optional<CardDetailsEntity> cardOpt = cardDetailsRepository.findByPan(body.cardNumber);
        if (!cardOpt.isPresent()) return APIResponse.error("Card not found");
        CardDetailsEntity card = cardOpt.get();
        try {
            // Build request; PinChangeRequest in your project likely has a new PIN field
            PinChangeRequest r = new PinChangeRequest();
            String newPin  = SensitiveDataUtil.decrypt(card.getNewPin(),env.SENSITIVE_DATA_ENCRYPTION_KEY);
            String currentPin = SensitiveDataUtil.decrypt(card.getOtac(), env.SENSITIVE_DATA_ENCRYPTION_KEY);
            r.setCardNumber(body.getCardNumber());
            r.setChannelId(card.getChannelId());
            r.setCurrentPIN(currentPin);
            r.setNewPIN(newPin);
            UbxResponse res = bcxService.pinChange(r);
            return isOk(res) ? APIResponse.ok(res) : APIResponse.error(res.getResponseMessage());
        } catch (Exception e) {
            return APIResponse.error(e.getMessage());
        }
    }

    /* ---------------------------
     * 6) ACTIVATE (Stage 6)
     * --------------------------- */
    @PostMapping("/card/activate")
    public APIResponse<UbxResponse> activate(@RequestBody CardNoOnly body) {
        try {
            UnblockCardRequest r = new UnblockCardRequest();
            r.setCardNumber(body.getCardNumber());
            r.setChannelId("CILANTRO");
            r.setInstitutionId(env.UBX_CLIENT_ID);
            UbxResponse res = bcxService.activateCard(r);
            return isOk(res) ? APIResponse.ok(res) : APIResponse.error(res.getResponseMessage());
        } catch (Exception e) {
            return APIResponse.error(e.getMessage());
        }
    }

    /* ---------------------------
     * UTILITIES
     * --------------------------- */
    private boolean isOk(UbxResponse res) {
        if (res == null) return false;
        String code = String.valueOf(res.getResponseCode());
        return "00".equalsIgnoreCase(code) || "0".equalsIgnoreCase(code);
    }

    private String safeUsername() {
        try {
            String u = SecurityContextHolder.getContext().getAuthentication().getName();
            return StringUtils.defaultIfBlank(u, "SYSTEM");
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    private String safeBranch(HttpSession session) {
        Object b = session != null ? session.getAttribute("branchCode") : null;
        return b != null ? String.valueOf(b) : "0";
    }

    /* ---------------------------
     * REQUEST DTOs for this controller
     * --------------------------- */
    @Data
    public static class IssueCardLinkRequest {
        private String cardNumber;
        private String accountNumber;
        private String msisdn;
        private String email;
        private String panExpireDate;

        // optional hints (if UI collects them later)
        private String customerName;
        private String shortName;
        private String customerRimNo;
        private String custId;
        private String category;
    }

    @Data
    public static class CardNoOnly {
        private String cardNumber;
    }

    @Data
    public static class RegisterCBSRequest {
        private String pan;
    }

    @Data
    public static class ChangePinRequest {
        private String cardNumber;
        private int pin;
        private String currentPin;
    }
}
