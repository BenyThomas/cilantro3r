/*
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.DTO.Ebanking.BCXCardCreationResp;
import com.DTO.Ebanking.CardRegistrationReq;
import com.DTO.ubx.*;
import com.config.SYSENV;
import com.dao.kyc.response.ors.ResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.*;
import com.models.ubx.*;
import com.repository.EbankingRepo;
import com.repository.ubx.CardActionStatusRepository;
import com.repository.ubx.CardDetailsRepository;
import com.repository.ubx.CustomerCardRequestRepository;
import com.security.SensitiveDataUtil;
import com.service.ubx.UrlEndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import philae.ach.TaResponse;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author melleji.mollel
 */
@Service
public class BCXService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCXService.class);
    private static final String GENERAL_ERROR_CODE = String.valueOf(96);
    @Autowired
    SYSENV systemVariables;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    KeyLoader keyLoader;
    @Autowired
    SYSENV env;
    @Autowired
    RestTemplate rest;
    @Autowired
    UrlEndpointService service;
    @Autowired
    private MailService mailInit;
    @Autowired
    CardDetailsRepository ubxSentRequestRepository;
    @Autowired
    TransferService transferService;
    @Autowired
    XapiWebService xapiWebService;
    @Autowired
    SMSService smsService;

    @Autowired
    @Lazy
    EbankingRepo ebankingRepo;
    @Autowired
    private CardDetailsRepository cardDetailsRepository;
    @Autowired
    private CardActionStatusRepository cardActionStatusRepository;
    @Autowired
    private CustomerCardRequestRepository customerCardRequestRepository;

    public String createPANRequestAndSendToBCX(String payLoad) {
        String clearReponse = "-1";
        try {
            LOGGER.info("Payload for pan gen: " + payLoad);
            String encContent = EncryptUtil.encryptRSAStr(payLoad, systemVariables.BCX_PGP_PRIVATE_KEY);
            String bcxPayloadData = "{\"data\":\"" + encContent + "\"}";
            String response = HttpClientService.sendReqToAPIBcx(bcxPayloadData, systemVariables.BCX_HTTP_URL, systemVariables.BCX_CLIENT_ID);
            LOGGER.info("response for pan gen .. {}", response);
            BCXCardCreationResp EncrytedCardResponse = objectMapper.readValue(response, BCXCardCreationResp.class);
            clearReponse = EncryptUtil.decryptRSAStr(EncrytedCardResponse.getData(), systemVariables.BCX_PGP_PUBLIC_KEY);
            LOGGER.info("REQUEST: {}=> RESPONSE: {}", payLoad, clearReponse);
        } catch (Exception ex) {
            clearReponse = "Exception: " + ex.getMessage();
            LOGGER.info("AN ERROR OCCURRED DURING CARD CREATION FROM BCX SIDE: PAYLOAD {}\nERROR:{}", payLoad, ex.getMessage());
        }
        return clearReponse;

    }

    public String createPANInitialPINBCX(String payLoad) {
        String clearReponse = "-1";
        try {
            LOGGER.info("Payload: " + payLoad);
            String encContent = EncryptUtil.encryptRSAStr(payLoad, systemVariables.BCX_PGP_PRIVATE_KEY);
            String bcxPayloadData = "{\"data\":\"" + encContent + "\"}";
            String response = HttpClientService.sendReqToAPIBcx(bcxPayloadData, systemVariables.BCX_HTTP_URL, systemVariables.BCX_CLIENT_ID);
            BCXCardCreationResp EncryptedCardResponse = objectMapper.readValue(response, BCXCardCreationResp.class);
            clearReponse = EncryptUtil.decryptRSAStr(EncryptedCardResponse.getData(), systemVariables.BCX_PGP_PUBLIC_KEY);
        } catch (JsonProcessingException ex) {
            LOGGER.info("AN ERROR OCCURRED DURING CARD CREATION FROM BCX SIDE: PAYLOAD {}\nERROR:{}", payLoad, ex.getMessage());
        }
        return clearReponse;

    }

    public String createPANInitialPINBCX_tes(String payLoad) {
        String clearReponse = "-1";
        try {
            LOGGER.info("Payload: " + payLoad);
            BCXCardCreationResp EncrytedCardResponse = objectMapper.readValue(payLoad, BCXCardCreationResp.class);
            clearReponse = EncryptUtil.decryptRSAStr(EncrytedCardResponse.getData(), systemVariables.BCX_PGP_PUBLIC_KEY);
        } catch (JsonProcessingException ex) {
            LOGGER.info("AN ERROR OCCURRED DURING CARD CREATION FROM BCX SIDE: PAYLOAD {}\nERROR:{}", payLoad, ex.getMessage());
        }
        return clearReponse;

    }

    public UbxResponse linkCardToAccount(CustomerInfoReq linkCardRequest) throws JsonProcessingException {
//        if("uat".equalsIgnoreCase(systemVariables.ACTIVE_PROFILE))return new UbxResponse("Success","00");
        CustomerInfo info = objectMapper.convertValue(linkCardRequest, CustomerInfo.class);
        info.setAccountType("10");
        CustomerCardRequest cReq = objectMapper.convertValue(linkCardRequest, CustomerCardRequest.class);
        CustomerCardRequest savedReq = customerCardRequestRepository.save(cReq);
        CardDetailsEntity savedRequest = saveRequest(linkCardRequest);
        List<CardDetailsEntity> cards = cardDetailsRepository.findRecentByAccountNumber(linkCardRequest.getAccountNumber());
        boolean existsWithDifferentCard =
                cards.stream().anyMatch(card -> !card.getPan().equals(linkCardRequest.getCardNumber()));
        if (!cards.isEmpty() && existsWithDifferentCard && cardDetailsRepository.existsByAccountNumberCreatedWithin30Days(linkCardRequest.getAccountNumber()) > 0) {
            String message = String.format("AC# %s is already linked to another card with PAN %s", linkCardRequest.getAccountNumber(), cards.get(0).getPan());
            return new UbxResponse(message, "01");
        }
        if (!CardValidator.validateCardNumber(linkCardRequest.getCardNumber())) {
            return new UbxResponse(String.format("Invalid card number: %s. Please check and try again.", linkCardRequest.getCardNumber()), "GENERAL_ERROR_CODE");
        }
        Optional<UrlEndpointEntity> endpoint = service.findEndpointByName("linkCard");
        if (!endpoint.isPresent()) {
            savedRequest.setStatus("F");
            ubxSentRequestRepository.save(savedRequest);
            return new UbxResponse("Invalid Endpoint Executed", GENERAL_ERROR_CODE);
        }

        if (!hasCardInsuranceChargeBalance(linkCardRequest.getAccountNumber())) {
            String error = "Visa Card Charge Balance is not sufficient";
            LOGGER.error("{} - {}", error, linkCardRequest.getAccountNumber());
            savedRequest.setStatus("F");
            ubxSentRequestRepository.save(savedRequest);
            return new UbxResponse(error, GENERAL_ERROR_CODE);
        }
        linkCardRequest.setCharge(env.VISA_CARD_REQUEST_CHARGE);
        String cardNo = SensitiveDataUtil.maskCardPan(linkCardRequest.getCardNumber());
        String accountNo = SensitiveDataUtil.maskAccount(linkCardRequest.getAccountNumber());
        beginStep(savedRequest, CardRegistrationStep.L,
                String.format("Linking PAN %s to AC %s", cardNo,accountNo ));
        LinkCardRequest request = objectMapper.convertValue(info, LinkCardRequest.class);
        UbxResponse response = sendRequestToUBX(request, endpoint, savedRequest);
        if (response.getResponseCode().equals("00")) {
           savedRequest.setUbxStatus(UBXStatus.LINKED);
            CardDetailsEntity savedCard = ubxSentRequestRepository.save(savedRequest);
            recordStep(savedCard,CardRegistrationStep.L,"SUCCESS",response.getResponseMessage(), savedReq);
        }else {
            recordStep(savedRequest,CardRegistrationStep.L,"FAILED",response.getResponseMessage(),savedReq);
            savedRequest.setStatus("F");
            ubxSentRequestRepository.save(savedRequest);

        }
        return response;
    }


    public UbxResponse activateCard(UnblockCardRequest request) throws JsonProcessingException {
//        if("uat".equalsIgnoreCase(systemVariables.ACTIVE_PROFILE))return new UbxResponse("Success","00");
        ReissueCardPinRequest rex = new ReissueCardPinRequest();
        rex.setCardNumber(request.getCardNumber());
        rex.setChannelId(request.getChannelId());
        rex.setUserIdentification(env.UBX_CLIENT_ID);
        rex.setInstitutionId(request.getInstitutionId());
        if (!CardValidator.validateCardNumber(request.getCardNumber())) {
            return new UbxResponse("Invalid Card Number", GENERAL_ERROR_CODE);
        }
        Optional<UrlEndpointEntity> endpoint = service.findEndpointByName("cardActivation");
        if (!endpoint.isPresent()) {
            return new UbxResponse("Invalid Endpoint Executed", GENERAL_ERROR_CODE);
        }
        Optional<CardDetailsEntity> optionalCardDetailsEntity = cardDetailsRepository.findByPan(request.getCardNumber());
        if (!optionalCardDetailsEntity.isPresent()) {
            return new UbxResponse("Card Number is Not Linked To Customer", GENERAL_ERROR_CODE);
        }
        CardDetailsEntity req = optionalCardDetailsEntity.get();
        if (checkCardStatusFromUBX(rex)) {
            req.setStatus("C");
            req.setStage("6");
            req.setUbxStatus(UBXStatus.ACTIVATED);
            recordStep(req,CardRegistrationStep.A,"SUCCESS","Card is already activated");
            ubxSentRequestRepository.save(req);
            return new UbxResponse("Card is already activated", "00");
        }
        req.setStatus("C");
        req.setIssuedBy(request.getChannelId());
        req.setIssuedDt(LocalDateTime.now());
        req.setIsAllowed("Y");
        req.setStage("6");
        CardDetailsEntity requestToUpdate = ubxSentRequestRepository.saveAndFlush(req);
        beginStep(requestToUpdate, CardRegistrationStep.A,
                String.format("Activating PAN %s", request.getCardNumber()));
        UbxResponse response = sendRequestToUBX(request, endpoint, requestToUpdate);
        if (response.getResponseCode().equals("00")) {
            requestToUpdate.setUbxStatus(UBXStatus.ACTIVATED);
            CardDetailsEntity savedCard = ubxSentRequestRepository.save(requestToUpdate);
            recordStep(savedCard,CardRegistrationStep.A,"SUCCESS",response.getResponseMessage());
        }else {
            recordStep(requestToUpdate,CardRegistrationStep.A,"FAILED",response.getResponseMessage());
            req.setStatus("F");
            ubxSentRequestRepository.save(req);
        }
        return response;
    }
    private UbxResponse sendRequestToUBX(Object request, Optional<UrlEndpointEntity> endpoint, CardDetailsEntity requestToUpdate) throws JsonProcessingException {

        LOGGER.info("Sending {} => {}", objectMapper.writeValueAsString(request), endpoint);
        try {
            LOGGER.info("Processing payload: {}", request);
            String payload = objectMapper.writeValueAsString(request);
            LOGGER.info("Payload Before Encryption: {}", payload);

            RequestWrapper encContent = EncryptUtil.encryptAndSignPayload(
                    payload,
                    env.UBX_CERT_PASSWORD,
                    keyLoader.loadPrivateKey(Paths.get(env.UBX_PRIVATE_KEY_PATH.getURI()))
            );

            String finalPayload = objectMapper.writeValueAsString(encContent);
            LOGGER.info("Payload After Encryption: {}", finalPayload);
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Date", DateUtil.getCurrentTimestamp());
            headers.add("CLIENT-ID", env.UBX_CLIENT_ID);

            HttpEntity<String> entity = new HttpEntity<>(finalPayload, headers);
            String url = endpoint.isPresent() ?endpoint.get().getEndpoint():"";
            // Send request
            ResponseEntity<ResponseWrapper> response = rest.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ResponseWrapper.class
            );

            LOGGER.info("Response status: {}", response.getStatusCode());
            LOGGER.info("Response Body: {}", Objects.requireNonNull(response.getBody()).getResponse());
            if (response.getStatusCode().equals(HttpStatus.OK) && response.getBody() != null) {
                // Decrypt and verify signature
                String decryptedResponse = EncryptUtil.decryptResponse(response.getBody().getResponse(), env.UBX_CERT_PASSWORD);

                String sanitized = decryptedResponse.replace("\r", "")     // Remove carriage returns
                        .replace("\t", "")     // Remove tabs
                        .replaceAll("\\s+$", "") // Trim trailing whitespaces
                        .replaceAll("[\\u0000-\\u001F]", "");

                boolean isVerified = EncryptUtil.isSignatureVerified(sanitized, response.getBody().getSignature(), keyLoader.loadPublicKey(Paths.get(env.UBX_PUBLIC_KEY_PATH.getURI())));

                if (isVerified) {
                    UbxResponse ubxResponse = objectMapper.readValue(sanitized, UbxResponse.class);
                    requestToUpdate.setResponseCode(String.valueOf(ubxResponse.getResponseCode()));
                    LOGGER.info("Card Details: {}", ubxResponse);
                    ubxSentRequestRepository.save(requestToUpdate);
                    return ubxResponse;
                } else {
                    String message = String.format("Signature verification failed for response: %s", decryptedResponse);
                    LOGGER.error(message);
                    requestToUpdate.setResponseCode(GENERAL_ERROR_CODE);
                    ubxSentRequestRepository.save(requestToUpdate);
                    return new UbxResponse("Error: Response signature verification failed.", GENERAL_ERROR_CODE);
                }
            } else {
                LOGGER.error("Failed. Response: {}", response);
                requestToUpdate.setResponseCode("GENERAL_ERROR_CODE");
                return new UbxResponse("Error occurred while linking the card. Please try again later.", GENERAL_ERROR_CODE);
            }

        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred: {}", e.getMessage(), e);
            return new UbxResponse("An unexpected error occurred. Please contact support", GENERAL_ERROR_CODE);
        }
    }

    public UbxResponse reissueCardPin(ReissueCardPinRequest request) throws JsonProcessingException {
//        if("uat".equalsIgnoreCase(systemVariables.ACTIVE_PROFILE))return new UbxResponse("Success","00","4545","1586");
        if (!CardValidator.validateCardNumber(request.getCardNumber())) {
            LOGGER.info("Invalid card number: {}", request.getCardNumber());
            return new UbxResponse("Invalid Card Number", GENERAL_ERROR_CODE);
        }
        Optional<UrlEndpointEntity> endpoint = service.findEndpointByName("reissueCardPin");
        if (!endpoint.isPresent()) {
            LOGGER.info("Endpoint not found");
            return new UbxResponse("Invalid Endpoint Executed", GENERAL_ERROR_CODE);
        }
        Optional<CardDetailsEntity> optionalCardDetailsEntity = ubxSentRequestRepository.findByPan(request.getCardNumber());
        if (!optionalCardDetailsEntity.isPresent()) {
            LOGGER.info("Card Details not found");
            return new UbxResponse("Card Number is not Linked to Customer", GENERAL_ERROR_CODE);
        }
        CardDetailsEntity info = optionalCardDetailsEntity.get();
        info.setStage("2");
        info.setStatus("PR");
        beginStep(info, CardRegistrationStep.PR, String.format("Reissuing PIN for PAN %s", SensitiveDataUtil.maskCardPan(request.getCardNumber())));
        request.setUserIdentification(env.UBX_CLIENT_ID);
        UbxResponse response = sendRequestToUBX(request, endpoint, info);
        LOGGER.info("RESPONSE CODE: {}, PIN: {}, STATUS: {} MESSAGE: {}", response.getResponseCode(), response.getOtac(), response.getStatus(), response.getResponseMessage());
        if ("00".equalsIgnoreCase(response.getResponseCode())) {
            //Sending pin email to user
            recordStep(info,CardRegistrationStep.PR,"SUCCESS",response.getResponseMessage());
            info.setUbxStatus(UBXStatus.PIN_ISSUED);
            info.setOtac(SensitiveDataUtil.encrypt(response.getOtac(),systemVariables.SENSITIVE_DATA_ENCRYPTION_KEY));
            ubxSentRequestRepository.save(info);
//            UbxResponse cbsResponse = issueCardToCBS(info, response.getOtac());
//            LOGGER.info("Response from CBS-RUBIKON: {}", cbsResponse);
//            if (cbsResponse != null) {
//                if ("0".equalsIgnoreCase(cbsResponse.getResponseCode())) {
//                    recordStep(info, CardRegistrationStep.R, "SUCCESS", cbsResponse.getResponseMessage());
//                    String reference = generateISOTransactionReference();
//                    beginStep(info, CardRegistrationStep.C, "Processing Card Issuance Charge");
//                    int charge = info.isCharged()? 0 :transferService.procChargeForVisaCard(reference, info.getAccountNo(), info.getCollectingBranch(), "TZS");
//                    if (charge != 0) {
//                        recordStep(info,CardRegistrationStep.C,"FAILED", cbsResponse.getResponseMessage());
//                        if (charge == 51) {
//                            LOGGER.info("Insufficient funds: {}", objectMapper.writeValueAsString(charge));
//                            return new UbxResponse("Insufficient funds", String.valueOf(charge));
//                        } else if (charge == 26) {
//                            LOGGER.info("Duplicate transaction: {}", objectMapper.writeValueAsString(charge));
//                            return new UbxResponse("Duplicate transaction", String.valueOf(charge));
//                        } else {
//                            LOGGER.error("General error has occurred in core banking: {}", objectMapper.writeValueAsString(charge));
//                            return new UbxResponse("General error has occurred in core banking", String.valueOf(charge));
//                        }
//
//                    }
//                    //Update charge card charge state
//                    info.setCharged(true);
//                    info.setStatus("P");
//                    info.setSavedToCbs(cbsResponse.getResponseMessage());
//                    info.setUbxStatus(UBXStatus.PIN_ISSUED);
//                    ubxSentRequestRepository.save(info);
//                    recordStep(info,CardRegistrationStep.C,"SUCCESS", "Card charged successfully");
//                    return cbsResponse;
//                }else {
//                    info.setStatus("F");
//                    info.setStage("2");
//                    CardDetailsEntity savedCard = ubxSentRequestRepository.save(info);
//                    recordStep(savedCard,CardRegistrationStep.R,"FAILED", cbsResponse.getResponseMessage());
//                    cbsResponse.setOtac(response.getOtac() !=null?response.getOtac():"");
//                    return cbsResponse;
//                }
//
//            }
//            recordStep(info,CardRegistrationStep.R,"FAILED", "Got A null response from CBS during registration");
            return response;
        }else {
            recordStep(info,CardRegistrationStep.PR,"FAILED",response.getResponseMessage());
        }
        return response;
    }

    public UbxResponse pinChange(PinChangeRequest request) throws JsonProcessingException {
        if (!CardValidator.validateCardNumber(request.getCardNumber())) {
            LOGGER.info("Invalid card number: {}", request.getCardNumber());
            return new UbxResponse("Invalid Card Number", GENERAL_ERROR_CODE);
        }
        Optional<UrlEndpointEntity> endpoint = service.findEndpointByName("changeCardPin");
        if (!endpoint.isPresent()) {
            LOGGER.info("Endpoint not found");
            return new UbxResponse("Invalid Endpoint Executed", GENERAL_ERROR_CODE);
        }
        Optional<CardDetailsEntity> optionalCardDetailsEntity = ubxSentRequestRepository.findByPan(request.getCardNumber());
        if (!optionalCardDetailsEntity.isPresent()) {
            LOGGER.info("Card Details not found");
            return new UbxResponse("Card Number is not Linked to Customer", GENERAL_ERROR_CODE);
        }
        CardDetailsEntity info = optionalCardDetailsEntity.get();
        beginStep(info, CardRegistrationStep.PC, String.format("Changing PIN for PAN %s", SensitiveDataUtil.maskCardPan(request.getCardNumber())));
        request.setUserIdentification(env.UBX_CLIENT_ID);
        if (request.getTrack2Data() == null) request.setTrack2Data(request.getCardNumber()+"=");
        UbxResponse response = sendRequestToUBX(request, endpoint, optionalCardDetailsEntity.get());
        if ("00".equalsIgnoreCase(response.getResponseCode())){
            info.setUbxStatus(UBXStatus.PIN_CHANGED);
            info.setStage("5");
            info.setStatus("PC");
            ubxSentRequestRepository.save(info);
        }
        recordStep(optionalCardDetailsEntity.get(),CardRegistrationStep.PC,"00".equalsIgnoreCase(response.getResponseCode())?"SUCCESS":"FAILED",response.getResponseMessage());
        return response;

    }
//    private void sendPinEmail(String email, UbxResponse response) {
//        String emailBody = String.format("Ndugu Mteja usajili wako umekamilika, Neno la Siri la muda ni %s. Tafadhali badilisha Neno la siri Ili Kuwezesha Kadi yako", response.getOtac());
//        LOGGER.info("Sending PIN {} to customer email: {}", response.getOtac(), emailBody);
//        Map<String, Object> form = new HashMap<>();
//        form.put("mailSubject", "TCB Bank VISA CARD INITIAL PIN");
//        form.put("mailFrom", "e-reports@tcbbank.co.tz");
//        form.put("mailTo", email);
//        form.put("mailCC", null);
//        form.put("mailBCC", null);
//        try {
//            mailInit.sendHtmlEmail(emailBody, form, null);
//        } catch (Exception e) {
//            LOGGER.info("Error while sending PIN {} to customer email: {}", response.getOtac(), e.getMessage());
//        }
//    }

    private CardDetailsEntity saveRequest(CustomerInfoReq info) {
        Optional<CardDetailsEntity> optCard = cardDetailsRepository.findByPan(info.getCardNumber());
        if (optCard.isPresent()) {
            CardDetailsEntity cardDetails = optCard.get();
            cardDetails.setAccountNo(info.getAccountNumber());
            cardDetails.setPan(info.getCardNumber());
            cardDetails.setCreatedDt(LocalDateTime.now());
            cardDetails.setCreatedBy(info.getCurrentUserName());
            cardDetails.setCustomerName(info.getCustomerName());
            cardDetails.setCustomerShortName(info.getFirstName());
            cardDetails.setCustomerRirmNo(info.getCustomerRim());
            cardDetails.setPhone(info.getMsisdn());
            cardDetails.setApprover1(info.getChannelId());
            cardDetails.setApprover1Dt(LocalDateTime.now());
            cardDetails.setApprover2Dt(LocalDateTime.now());
            cardDetails.setIssuedDt(LocalDateTime.now());
            cardDetails.setIssuedBy(info.getCurrentUserName());
            cardDetails.setCardexpireDt(info.getPanExpireDate());
            cardDetails.setApprover1Dt(LocalDateTime.now());
            cardDetails.setStatus("L");
            cardDetails.setStage("1");
            cardDetails.setBin(info.getCardNumber().substring(0, 8));
            cardDetails.setCharge(Integer.parseInt(info.getCharge() == null ? "0" : info.getCharge()));
            cardDetails.setOriginatingBranch(info.getBranchCode());
            cardDetails.setCollectingBranch(info.getBranchCode());
            cardDetails.setEmail(info.getEmail());
            cardDetails.setCustid(info.getCustId());
            cardDetails.setCustomerRirmNo(info.getCustomerRim());
            cardDetails.setCustomerCategory(info.getCategory());
            cardDetails.setAddress1("NA");
            cardDetails.setAddress2("NA");
            cardDetails.setAddress3("NA");
            cardDetails.setAddress4("NA");
            cardDetails.setIsAllowed("Y");
            cardDetails.setChannelId(info.getChannelId());
            cardDetails.setReference(info.getChannelId() + generateISOTransactionReference());
            cardDetails.setNationalId(info.getNationalId());
            cardDetails.setUbxStatus(UBXStatus.INITIATED);
            return cardDetailsRepository.save(cardDetails);
        }
        CardDetailsEntity cardDetails = new CardDetailsEntity();
        cardDetails.setAccountNo(info.getAccountNumber());
        cardDetails.setPan(info.getCardNumber());
        cardDetails.setCreatedDt(LocalDateTime.now());
        cardDetails.setCreatedBy(info.getCurrentUserName());
        cardDetails.setCustomerName(info.getCustomerName());
        cardDetails.setCustomerShortName(info.getFirstName());
        cardDetails.setCustomerRirmNo(info.getCustomerRim());
        cardDetails.setCustid(info.getCustId());
        cardDetails.setPhone(info.getMsisdn());
        cardDetails.setIssuedDt(LocalDateTime.now());
        cardDetails.setIssuedBy(info.getCurrentUserName());
        cardDetails.setCardexpireDt(info.getPanExpireDate());
        cardDetails.setApprover1Dt(LocalDateTime.now());
        cardDetails.setStatus("I");
        cardDetails.setStage("0");
        cardDetails.setAddress1("NA");
        cardDetails.setAddress2("NA");
        cardDetails.setAddress3("NA");
        cardDetails.setAddress4("NA");
        cardDetails.setBin(info.getCardNumber().substring(0, 8));
        cardDetails.setCharge(Integer.parseInt(info.getCharge() == null ? "0" : info.getCharge()));
        cardDetails.setOriginatingBranch(info.getBranchCode());
        cardDetails.setCollectingBranch(info.getBranchCode());
        cardDetails.setEmail(info.getEmail());
        cardDetails.setIsAllowed("Y");
        cardDetails.setNationalId(info.getNationalId());
        cardDetails.setCustomerCategory(info.getCategory());
        cardDetails.setApprover2Dt(LocalDateTime.now());
        cardDetails.setApprover2(info.getCurrentUserName());
        cardDetails.setApprover1Dt(LocalDateTime.now());
        cardDetails.setApprover1(info.getCurrentUserName());
        cardDetails.setDispatchedBy(info.getCurrentUserName());
        cardDetails.setDispatchedDt(LocalDateTime.now());
        cardDetails.setHqReceivedBy(info.getCurrentUserName());
        cardDetails.setHqReceivedDt(LocalDateTime.now());
        cardDetails.setChannelId(info.getChannelId());
        cardDetails.setUbxStatus(UBXStatus.INITIATED);
        cardDetails.setNewPin(SensitiveDataUtil.encrypt(info.getNewPin(), systemVariables.SENSITIVE_DATA_ENCRYPTION_KEY));
        cardDetails.setReference(info.getChannelId() + generateISOTransactionReference());
        return ubxSentRequestRepository.save(cardDetails);
    }

    private String generateISOTransactionReference() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNumber = 1000 + random.nextInt(9000);
        return timestamp + randomNumber;
    }

    public UbxResponse issueCardToCBS(CardDetailsEntity card, String pin) {
        CardRegistrationReq reqCM = new CardRegistrationReq();
        reqCM.setAccountNo(card.getAccountNo());
        reqCM.setCustomerName(card.getCustomerName());
        reqCM.setPan(card.getPan());
        reqCM.setReference(card.getReference());
        reqCM.setOneTimePassword(pin);
        reqCM.setCustomerName(card.getCustomerName());
        reqCM.setCustomerPhoneNumber(card.getPhone());
        reqCM.setTerminalName(card.getCreatedBy());
        reqCM.setTerminalId(card.getCreatedBy());
        reqCM.setPanExpireDate(card.getCardexpireDt());
        reqCM.setNotify(true);
        beginStep(card, CardRegistrationStep.R, "Issuing card to CBS");
        UbxResponse savedToCBS = ebankingRepo.issueVisaCardToCoreBanking(reqCM, pin, card.getCreatedBy());
        LOGGER.info("issueVisaCardToCoreBanking RESPONSE: {}", savedToCBS);
        return savedToCBS;
    }

    private boolean checkCardStatusFromUBX(ReissueCardPinRequest request) {
        Optional<UrlEndpointEntity> endpoint = service.findEndpointByName("cardRecord");
        if (!endpoint.isPresent()) return false;
        try {
            String payload = objectMapper.writeValueAsString(request);
            LOGGER.info("Payload Before Encryption: {}",payload);
            RequestWrapper encContent = EncryptUtil.encryptAndSignPayload(
                    payload,
                    env.UBX_CERT_PASSWORD,
                    keyLoader.loadPrivateKey(Paths.get(env.UBX_PRIVATE_KEY_PATH.getURI()))
            );

            String finalPayload = objectMapper.writeValueAsString(encContent);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Date", DateUtil.getCurrentTimestamp());
            headers.add("CLIENT-ID", env.UBX_CLIENT_ID);

            HttpEntity<String> entity = new HttpEntity<>(finalPayload, headers);

            // Send request
            ResponseEntity<ResponseWrapper> response = rest.exchange(
                    endpoint.get().getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    ResponseWrapper.class
            );

            LOGGER.info("Response status: {}", response.getStatusCode());
            LOGGER.info("Response Body: {}", Objects.requireNonNull(response.getBody()).getResponse());
            if (response.getStatusCode().equals(HttpStatus.OK) && response.getBody() != null) {

                // Decrypt and verify signature
                String decryptedResponse = EncryptUtil.decryptResponse(response.getBody().getResponse(), env.UBX_CERT_PASSWORD);

                String sanitized = decryptedResponse.replace("\r", "")     // Remove carriage returns
                        .replace("\t", "")     // Remove tabs
                        .replaceAll("\\s+$", "") // Trim trailing whitespaces
                        .replaceAll("[\\u0000-\\u001F]", "");

                LOGGER.info("Response After Decryption: {}", sanitized);
                boolean isVerified = EncryptUtil.isSignatureVerified(sanitized, response.getBody().getSignature(), keyLoader.loadPublicKey(Paths.get(env.UBX_PUBLIC_KEY_PATH.getURI())));

                if (isVerified) {
                    CardDetailsResponse ubxResponse = objectMapper.readValue(sanitized, CardDetailsResponse.class);
                    if (ubxResponse !=null){
                        return "1".equalsIgnoreCase(ubxResponse.getCardStatus());
                    }
                    return false;
                } else {
                    String message = String.format("Signature verification failed for response: %s", decryptedResponse);
                    LOGGER.error(message);
                    return false;
                }
            } else {
                LOGGER.error("Failed. Response: {}", response);
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred: {}", e.getMessage(), e);
            return false;
        }
    }

//    @Transactional
//    public UbxResponse processInstantIssuance(CustomerInfoReq req) {
//        // 1) LINK
//        UbxResponse linkRes = trackStep(null, CardRegistrationStep.L, () -> linkCardToAccount(req));
//        if (!isOk(linkRes)) return linkRes;
//
//        // Resolve card after linking
//        Optional<CardDetailsEntity> opt = cardDetailsRepository.findByPan(req.getCardNumber());
//        if (!opt.isPresent()) return new UbxResponse("Linked card not found after LINK step", "96");
//        CardDetailsEntity card = opt.get();
//
//        // 2) ACTIVATE
//        UnblockCardRequest activate = new UnblockCardRequest();
//        activate.setInstitutionId(env.UBX_CLIENT_ID);
//        activate.setCardNumber(card.getPan());
//        activate.setChannelId(card.getChannelId());
//        activate.setUserIdentification(env.UBX_USER_ID);
//        UbxResponse actRes = trackStep(card, CardRegistrationStep.A, () -> activateCard(activate));
//        if (!isOk(actRes)) return actRes;
//
//        // 3) PIN REISSUE
//        ReissueCardPinRequest pr = new ReissueCardPinRequest();
//        pr.setCardNumber(card.getPan());
//        pr.setChannelId(card.getChannelId());
//        pr.setUserIdentification(env.UBX_USER_ID);
//        pr.setInstitutionId(env.UBX_CLIENT_ID);
//        UbxResponse pinRes = trackStep(card, CardRegistrationStep.PR, () -> reissueCardPin(pr));
//        if (!isOk(pinRes)) return pinRes;
//
//        // 4) ISSUE TO CBS + CHARGE (R)
//        UbxResponse cbsRes = trackStep(card, CardRegistrationStep.R, () -> {
//            // issue to CBS already done inside reissueCardPin() on success branch,
//            // but for idempotency we do a guarded call here ONLY if savedToCbs is null
//            if (card.getSavedToCbs() == null || card.getSavedToCbs().isEmpty()) {
//                // We must have OTAC by now (set in reissueCardPin)
//                String pin = card.getOtac() != null ? card.getOtac() : card.getPan(); // fallback like your code path
//                return issueCardToCBS(card, pin);
//            }
//            return new UbxResponse("CBS already updated", "00");
//        });
//        if (!isOk(cbsRes)) return cbsRes;
//
//        // 5) NOTIFY (N)
//        UbxResponse notifyRes = trackStep(card, CardRegistrationStep.N, () -> sendNotification(card));
//        return notifyRes;
//    }

//    /**
//     * Orchestrate remaining steps for an existing card (used by "Re-complete").
//     * It finds the first failed step and runs from there forward.
//     */
//    @Transactional
//    public UbxResponse processInstantIssuance(Long cardId) {
//        Optional<CardDetailsEntity> cardOpt = cardDetailsRepository.findById(cardId);
//        if (!cardOpt.isPresent()) return new UbxResponse("Card is Not Available", "404");
//        CardDetailsEntity card = cardOpt.get();
//
//        CardRegistrationStep failedFrom = getFailedStep(cardId);
//        if (failedFrom == null) {
//            // If nothing failed, try to detect incomplete path by checking which steps succeeded
//            failedFrom = firstIncompleteStep(cardId);
//            if (failedFrom == null) {
//                return new UbxResponse("No steps pending for this card", "00");
//            }
//        }
//
//        List<CardRegistrationStep> remaining = getRemainingSteps(failedFrom);
//        for (CardRegistrationStep step : remaining) {
//            boolean alreadySucceeded = cardActionStatusRepository.existsByCardIdAndStepAndStatus(cardId, step, "SUCCESS");
//            if (alreadySucceeded) continue;
//
//            UbxResponse r = trackStep(card, step, () -> executeStepResponse(card, step));
//            if (!isOk(r)) return r;
//        }
//        return new UbxResponse("Card processing completed successfully", "00");
//    }

    // Determine the first step that hasn’t succeeded yet (in sequence order)
    private CardRegistrationStep firstIncompleteStep(Long cardId) {
        for (CardRegistrationStep s : CardRegistrationStep.values()) {
            if (!cardActionStatusRepository.existsByCardIdAndStepAndStatus(cardId, s, "SUCCESS")) {
                return s;
            }
        }
        return null;
    }

//    // Execute single step and return UbxResponse (not just boolean)
//    private UbxResponse executeStepResponse(CardDetailsEntity card, CardRegistrationStep step) throws Exception {
//        switch (step) {
//            case L: {
//                Optional<CustomerCardRequest> existingReq = customerCardRequestRepository
//                        .findByCardNumberAndAccountNumber(card.getPan(), card.getAccountNo());
//                if (!existingReq.isPresent())
//                    return new UbxResponse("Original request not found for LINK", "96");
//                CustomerInfoReq request = objectMapper.convertValue(existingReq.get(), CustomerInfoReq.class);
//                return linkCardToAccount(request);
//            }
//            case A: {
//                UnblockCardRequest r = new UnblockCardRequest();
//                r.setInstitutionId(env.UBX_CLIENT_ID);
//                r.setCardNumber(card.getPan());
//                r.setChannelId(card.getChannelId());
//                r.setUserIdentification(env.UBX_USER_ID);
//                return activateCard(r);
//            }
//            case PR: {
//                ReissueCardPinRequest rc = new ReissueCardPinRequest();
//                rc.setCardNumber(card.getPan());
//                rc.setChannelId(card.getChannelId());
//                rc.setUserIdentification(env.UBX_USER_ID);
//                rc.setInstitutionId(env.UBX_CLIENT_ID);
//                return reissueCardPin(rc);
//            }
//            case R: {
//                String pin = (card.getOtac() != null && !card.getOtac().isEmpty()) ? card.getOtac() : card.getPan();
//                return issueCardToCBS(card, pin);
//            }
//            case N: {
//                return sendNotification(card);
//            }
//            default:
//                return new UbxResponse("Unsupported step: " + step, "96");
//        }
//    }

    private boolean isOk(UbxResponse r) {
        return r != null && "00".equalsIgnoreCase(r.getResponseCode());
    }

    // ====== NEW: generic step tracker (BEGIN/END with attempts, timing, meta) ======

    @FunctionalInterface
    private interface StepExecutable {
        UbxResponse run() throws Exception;
    }

    private UbxResponse trackStep(CardDetailsEntity card, CardRegistrationStep step, StepExecutable exec) {
        // Count previous attempts for this step
        long attempts = card != null
                ? cardActionStatusRepository.countByCardIdAndStep(card.getId(), step)
                : 0L;
        int attemptNo = (int) attempts + 1;

        LocalDateTime started = LocalDateTime.now();
        Long actionId = beginEvent(card, step, attemptNo, started, null); // meta null for now

        try {
            UbxResponse res = exec.run();

            LocalDateTime ended = LocalDateTime.now();
            long durationMs = Duration.between(started, ended).toMillis();
            if (isOk(res)) {
                endEventSuccess(actionId, card, step, attemptNo, started, ended, durationMs, res.getResponseMessage());
            } else {
                endEventFailure(actionId, card, step, attemptNo, started, ended, durationMs,
                        (res != null ? res.getResponseMessage() : "NULL response"));
            }
            return res;
        } catch (Exception e) {
            LocalDateTime ended = LocalDateTime.now();
            long durationMs = Duration.between(started, ended).toMillis();
            endEventFailure(actionId, card, step, attemptNo, started, ended, durationMs, e.getMessage());
            LOGGER.error("Step {} failed due to: {}", step, e.getMessage(), e);
            return new UbxResponse("Exception at step " + step + ": " + e.getMessage(), "96");
        }
    }

    // ====== Tracking persistence wrappers over your CardActionStatusRepository ======

    /**
     * BEGIN marker. Returns the persisted action id (if available) to update on END.
     */
    private Long beginEvent(CardDetailsEntity card,
                            CardRegistrationStep step,
                            int attemptNo,
                            LocalDateTime startedAt,
                            String metaJson) {
        try {
            CardActionStatus action = new CardActionStatus();
            action.setCardId(card != null ? card.getId() : null);
            action.setStep(step);
            action.setStatus("BEGIN");
            action.setMessage("Begin step " + step + " attempt #" + attemptNo);
            // OPTIONAL COLUMN: action.setAttemptNo(attemptNo);
            // OPTIONAL COLUMN: action.setStartedAt(startedAt);
            // OPTIONAL COLUMN: action.setMetaJson(metaJson);
            action.setUpdatedAt(startedAt);
            action = cardActionStatusRepository.save(action);
            return action.getId();
        } catch (Exception e) {
            LOGGER.warn("beginEvent failed to persist BEGIN marker: {}", e.getMessage());
            return null;
        }
    }

    private void endEventSuccess(Long actionId,
                                 CardDetailsEntity card,
                                 CardRegistrationStep step,
                                 int attemptNo,
                                 LocalDateTime startedAt,
                                 LocalDateTime endedAt,
                                 long durationMs,
                                 String message) {
        try {
            // Log SUCCESS as a fresh row for clarity (BEGIN and END are separate rows)
            CardActionStatus action = new CardActionStatus();
            action.setCardId(card != null ? card.getId() : null);
            action.setStep(step);
            action.setStatus("SUCCESS");
            action.setMessage(summarizeMessage("SUCCESS", step, attemptNo, startedAt, endedAt, durationMs, message));
            // OPTIONAL COLUMNs:
            // action.setAttemptNo(attemptNo);
            // action.setStartedAt(startedAt);
            // action.setEndedAt(endedAt);
            // action.setDurationMs(durationMs);
            action.setUpdatedAt(endedAt);
            cardActionStatusRepository.save(action);
        } catch (Exception e) {
            LOGGER.warn("endEventSuccess failed to persist: {}", e.getMessage());
        }
    }

    private void endEventFailure(Long actionId,
                                 CardDetailsEntity card,
                                 CardRegistrationStep step,
                                 int attemptNo,
                                 LocalDateTime startedAt,
                                 LocalDateTime endedAt,
                                 long durationMs,
                                 String errorMessage) {
        try {
            CardActionStatus action = new CardActionStatus();
            action.setCardId(card != null ? card.getId() : null);
            action.setStep(step);
            action.setStatus("FAILED");
            action.setMessage(summarizeMessage("FAILED", step, attemptNo, startedAt, endedAt, durationMs, errorMessage));
            // OPTIONAL COLUMNs:
            // action.setAttemptNo(attemptNo);
            // action.setStartedAt(startedAt);
            // action.setEndedAt(endedAt);
            // action.setDurationMs(durationMs);
            action.setUpdatedAt(endedAt);
            cardActionStatusRepository.save(action);
        } catch (Exception e) {
            LOGGER.warn("endEventFailure failed to persist: {}", e.getMessage());
        }
    }

    private String summarizeMessage(String status,
                                    CardRegistrationStep step,
                                    int attemptNo,
                                    LocalDateTime startedAt,
                                    LocalDateTime endedAt,
                                    long durationMs,
                                    String msg) {
        // message is human-friendly and self-contained
        return String.format(
                "%s %s (attempt #%d) | started=%s ended=%s durationMs=%d | msg=%s",
                status, step.name(), attemptNo,
                startedAt != null ? startedAt : "-",
                endedAt != null ? endedAt : "-",
                durationMs,
                msg != null ? msg : "-"
        );
    }

    private boolean hasCardInsuranceChargeBalance(String account) throws JsonProcessingException {
        TaResponse taResponse = xapiWebService.accountBalance(account, StringUtils.generateIsoTransactionReference());
        LOGGER.info("CHECKING ACCOUNT BALANCE RESPONSE: {}", objectMapper.writeValueAsString(taResponse));
        //this is not working
        //return taResponse.getAvailableBalance().compareTo(BigDecimal.valueOf(Long.parseLong(env.VISA_CARD_MIN_AVAILABLE_BALANCE))) >= 0;
        BigDecimal available = taResponse.getAvailableBalance() != null? taResponse.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal minRequired = BigDecimal.valueOf(Long.parseLong(env.VISA_CARD_MIN_AVAILABLE_BALANCE));
        // If minimum is 0, allow any balance (including negative)
        if (minRequired.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        // Otherwise, enforce normal check
        return available.compareTo(minRequired) >= 0;
    }
    public void recordStep(CardDetailsEntity card, CardRegistrationStep step, String status, String message) {
        CardActionStatus action = new CardActionStatus();
        action.setCardId(card.getId());
        action.setStep(step);
        action.setStatus(status);
        action.setMessage(message);
        action.setUpdatedAt(LocalDateTime.now());
        cardActionStatusRepository.save(action);
    }
    public void recordStep(CardDetailsEntity card, CardRegistrationStep step, String status, String message,CustomerCardRequest req) {
        CardActionStatus action = new CardActionStatus();
        action.setCardId(card.getId());
        action.setStep(step);
        action.setStatus(status);
        action.setMessage(message);
        action.setRequestId(req.getId());
        action.setUpdatedAt(LocalDateTime.now());
        cardActionStatusRepository.save(action);
    }

    public ResponseDTO<List<FailedCardDTO>> getAllFailedCards() {
        List<CardActionStatus> statuses = cardActionStatusRepository.findByStatus("FAILED");
        // Extract distinct card IDs
        List<Long> cardIds = statuses.stream()
                .map(CardActionStatus::getCardId)
                .distinct()
                .collect(Collectors.toList());
        // Fetch cards in batch
        Map<Long, CardDetailsEntity> cardMap = cardDetailsRepository.findAllById(cardIds)
                .stream()
                .collect(Collectors.toMap(CardDetailsEntity::getId, Function.identity()));
        // Merge card + status info into DTO
        List<FailedCardDTO> failedCards = statuses.stream()
                .map(status -> {
                    CardDetailsEntity card = cardMap.get(status.getCardId());
                    if (card == null) return null;
                    return new FailedCardDTO(
                            card.getId(),
                            card.getPan(),
                            card.getAccountNo(),
                            card.getCustomerName(),
                            card.getOriginatingBranch(),
                            status.getStep().name(),
                            status.getMessage(),
                            status.getUpdatedAt()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ResponseDTO<>(true, "Fetched failed cards", failedCards,200);
    }

//    public UbxResponse resumeCardProcess(Long cardId) {
//        Optional<CardDetailsEntity> card = cardDetailsRepository.findById(cardId);
//        if (!card.isPresent())return new UbxResponse("Card is Not Available","404");
//        CardRegistrationStep failedStep = getFailedStep(cardId);
//        if (failedStep == null) return new UbxResponse("Card has no failed steps to resume", "99");
//        List<CardRegistrationStep> remainingSteps = getRemainingSteps(failedStep);
//
//        for (CardRegistrationStep step : remainingSteps) {
//            boolean alreadySucceeded = cardActionStatusRepository.existsByCardIdAndStepAndStatus(cardId, step, "SUCCESS");
//            if (alreadySucceeded) continue;
//            boolean success = executeStep(card.get(), step);
//            if (!success) {
//                return new UbxResponse("Card processing failed at step: " + step.name(), "01");
//            }
//        }
//
//        return new UbxResponse("Card processing completed successfully", "00");
//    }
    public List<CardRegistrationStep> getRemainingSteps(CardRegistrationStep failedStep) {
        return Arrays.stream(CardRegistrationStep.values())
                .filter(step -> step.ordinal() >= failedStep.ordinal())
                .collect(Collectors.toList());
    }
//    private boolean executeStep(CardDetailsEntity card, CardRegistrationStep step) {
//        try {
//            switch (step) {
//                case L:
//                    Optional<CustomerCardRequest> existingReq = customerCardRequestRepository.findByCardNumberAndAccountNumber(card.getPan(),card.getAccountNo());
//                    if (!existingReq.isPresent()) return false;
//                    CustomerInfoReq request = objectMapper.convertValue(existingReq.get(), CustomerInfoReq.class);
//                    return "00".equalsIgnoreCase(linkCardToAccount(request).getResponseCode());
//                case A:
//                    UnblockCardRequest r = new UnblockCardRequest();
//                    r.setInstitutionId(env.UBX_CLIENT_ID);
//                    r.setCardNumber(card.getPan());
//                    r.setChannelId(card.getChannelId());
//                    r.setUserIdentification(env.UBX_USER_ID);
//                    return "00".equalsIgnoreCase(activateCard(r).getResponseCode());
//                case PR:
//                    ReissueCardPinRequest rc = new ReissueCardPinRequest();
//                    rc.setCardNumber(card.getPan());
//                    rc.setChannelId(card.getChannelId());
//                    rc.setUserIdentification(env.UBX_USER_ID);
//                    rc.setInstitutionId(env.UBX_CLIENT_ID);
//                    return "00".equalsIgnoreCase(reissueCardPin(rc).getResponseCode());
//                case R:
//                    return "0".equalsIgnoreCase(issueCardToCBS(card,card.getPan()).getResponseCode());
//                case N:
//                    return "00".equalsIgnoreCase(sendNotification(card).getResponseCode());
//                default:
//                    return false;
//            }
//        } catch (Exception e) {
//            LOGGER.error("Step {} failed due to: {}", step, e.getMessage());
//            recordStep(card, step, "FAILED", e.getMessage());
//            return false;
//        }
//    }

//    public UbxResponse sendNotification(CardDetailsEntity card) {
//        SMSDTO smsDTO = new SMSDTO();
//        String msg = String.format(env.INSTANT_VISA_MESSAGE,card.getOtac());
//        smsDTO.setMessage(msg);
//        smsDTO.setPhone(card.getPhone());
//        LOGGER.error("PIN MESSAGE: {} PHONE: {}",msg, smsDTO.getPhone());
//        ResponseDTO<?> smsRes = smsService.sendSMS(smsDTO);
//        recordStep(card,CardRegistrationStep.N,200==smsRes.getResultCode()?"SUCCESS":"FAILED",smsRes.getMessage());
//        return new UbxResponse(smsRes.getMessage(),200==smsRes.getResultCode()?"00":"96");
//    }

//    public CardRegistrationStep getFailedStep(Long cardId) {
//        return cardActionStatusRepository.findByCardIdAndStatus(cardId, "FAILED")
//                .stream()
//                .findFirst()
//                .map(CardActionStatus::getStep)
//                .orElse(null);
//    }


    public void beginStep(CardDetailsEntity card, CardRegistrationStep step, String msg) {
        try {
            CardActionStatus action = new CardActionStatus();
            action.setCardId(card != null ? card.getId() : null);
            action.setStep(step);
            action.setStatus("INITIATED");
            action.setMessage(msg != null ? msg : ("Initiated " + step.name()));
            action.setUpdatedAt(LocalDateTime.now());
            cardActionStatusRepository.save(action);
        } catch (Exception e) {
            LOGGER.warn("Initiation persist failed: {}", e.getMessage());
        }
    }


}
