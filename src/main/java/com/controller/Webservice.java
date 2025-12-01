/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.AccountNameQueryResp;
import com.DTO.EMkopoReq;
import com.DTO.TransactionCheckReq;
import com.DTO.salary.PAYROLL;
import com.DTO.ubx.*;

import com.config.SYSENV;
import com.dao.kyc.response.ors.ResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.APIResponse;
import com.helper.Mapper;
import com.helper.SignRequest;
import com.lowagie.text.DocumentException;
import com.models.Transfers;
import com.models.ubx.CardActionStatus;
import com.models.ubx.CardDetailsEntity;
import com.repository.*;
import com.queue.QueueProducer;
import com.repository.salary.SalaryProcessingRepository;
import com.repository.tips.TipsRepository;
import com.repository.ubx.CardActionStatusRepository;
import com.repository.ubx.CardDetailsRepository;
import com.service.BCXService;
import com.service.TransferService;
import com.service.XMLParserService;
import com.service.kyc.ors.CompanyService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import static com.helper.Mapper.toSummary;

/**
 *
 * @author melleji.mollel
 */
@RestController
@RequestMapping("/api/")
public class Webservice {

    @Autowired
    WebserviceRepo webserviceRepo;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebserviceRepo.class);
    @Autowired
    QueueProducer queueProducer;
    @Autowired
    EftRepo eftRepo;

    @Autowired
    PensionRepo pensionRepo;

    @Autowired
    SwiftRepository swiftRepository;

    @Autowired
    TipsRepository tipsRepository;

    @Autowired
    BCXService bcxService;

    @Autowired
    CreditRepo creditRepo;

    @Autowired
    SalaryProcessingRepository salaryProcessingRepository;

    @Autowired
    CompanyService companyService;

    @Autowired
    TransferService transferService;

    @Autowired
    TransfersRepository transfersRepository;

    @Autowired
    Settings_m settingsRepo;

    @Autowired
    SYSENV systemVariables;
    @Autowired
    User user;
    @Autowired
    KafkaTemplate<String, CustomerInfoReq> kafkaTemplate;

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private CardActionStatusRepository cardActionStatusRepository;
    @Autowired
    private CardDetailsRepository cardDetailsRepository;
    @Autowired
    private CardSpecs cardSpecs;

    @RequestMapping(value = {"fxValidation"}, method = RequestMethod.POST, produces = "application/xml")
    public String fxValidation(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.fxValidation(payLoad);
    }

    @RequestMapping(value = {"fxRequest"}, method = RequestMethod.POST, produces = "application/xml")
    public String fxRequest(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.fxValidation(payLoad);
    }

    @RequestMapping(value = {"bankList"}, method = RequestMethod.POST, produces = "application/xml")
    public String banksList(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.banksList(payLoad);
    }

    @RequestMapping(value = {"transferPayment"}, method = RequestMethod.POST, produces = "application/xml")
    public String transferPayment(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res, HttpServletRequest req) throws IOException {
        LOGGER.info("Client: {} TRANSFER REQ:{}", req.getRemoteHost(), payLoad);
        return webserviceRepo.transferPayment(payLoad);
    }

    @RequestMapping(value = {"bookTransfer"}, method = RequestMethod.POST, produces = "application/xml")
    public String bookTransferPayments(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res, HttpServletRequest req) throws IOException {
        LOGGER.info("Client: {} TRANSFER REQ:{}", req.getRemoteHost(), payLoad);
        return webserviceRepo.bookTransferPayments(payLoad);
    }

    @RequestMapping(value = {"transfer2Wallet"}, method = RequestMethod.POST, produces = "application/xml")
    public String transfer2Wallet(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.transfer2Wallet(payLoad);
    }

    @RequestMapping(value = {"transferPaymentTest"}, method = RequestMethod.POST, produces = "application/xml")
    public String transferPaymentTest(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.transferPaymentTest(payLoad);
    }

    @RequestMapping(value = {"batchTransferPayment"}, method = RequestMethod.POST, produces = "application/xml")
    public String batchTransferPayment(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.batchPaymentTransfer(payLoad);
    }

//    @RequestMapping(value = {"bookTransfer"}, method = RequestMethod.POST, produces = "application/xml")
//    public String bookTransfer(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
//        return webserviceRepo.bookTransfer(payLoad);
//    }
    @RequestMapping(value = {"utilityNamequery"}, method = RequestMethod.POST, produces = "application/xml")
    public String walletTransfer(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.utilityNameQuery(payLoad);
    }

    @RequestMapping(value = {"lukuCallback"}, method = RequestMethod.POST, produces = "application/xml")
    public String lukuCallback(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.lukuGatewayCallback(payLoad);
    }

    @RequestMapping(value = {"mpesaCallback"}, method = RequestMethod.POST, produces = "application/xml")
    public String mpesaCallback(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        return webserviceRepo.mpesaGatewayCallback(payLoad);
    }

    @RequestMapping(value = {"bot/incoming"}, method = RequestMethod.POST, produces = "application/xml")
    public String botIncoming(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res, HttpServletRequest req) throws IOException, DocumentException, ParseException {
        LOGGER.info("REQUEST FROM BOT SOURCE IP ADDRESS:{} Payload: {}",req.getRemoteHost(), payLoad);
//        webserviceRepo.processBoTRTGSIncoming(payLoad);
//        String trimmed = payLoad.trim();
//        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
//
//        } else {
            return swiftRepository.processBoTRTGSIncoming(payLoad, "BOT-VPN");
//        }
    }

    @RequestMapping(value = {"bot/saveIsoDailyToken"}, method = RequestMethod.POST, produces = "application/xml")
    public String receiveBotIsoDailyToken(@RequestBody(required = false) String token, HttpServletRequest req) {
        LOGGER.info("REQUEST FROM KIPAYMENT: {} Payload: {}", req.getRemoteHost(), token);
        int result = swiftRepository.saveBoTIsoDailyToken(token);
        if (result != -1) {
            return "<payload><responseCode>0</responseCode><message>Token saved successfully!</message></payload>";
        } else {
            return "<payload><responseCode>99</responseCode><message>Error saving token!</message></payload>";
        }
    }

    @RequestMapping(value = {"bot/retrieveIsoDailyToken"}, method = RequestMethod.GET, produces = "application/xml")
    public String retrieveBotIsoDailyToken() {
        String result = swiftRepository.getBoTIsoDailyToken();
        if (!Objects.equals(result, "-1")) {
            return "<payload><responseCode>0</responseCode><message>" + result + "</message></payload>";
        } else {
            return "<payload><responseCode>99</responseCode><message>Error retrieving token!</message></payload>";
        }
    }

    @RequestMapping(value = {"swift/incoming"}, method = RequestMethod.POST, produces = "application/xml")
    public String swiftSTPIncoming(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) {
        LOGGER.info("REQUEST FROM SWIFT<->KPRINTER: {}", payLoad);
        try {
            if (payLoad.contains("{451:0}}")) {
                payLoad = payLoad.split("\\{451:0}}")[1];
            }
            if (payLoad.contains("{451:1}}")) {
                payLoad = payLoad.split("\\{451:1}}")[1];
            }
            return swiftRepository.processBoTRTGSIncoming(payLoad, "SWIFT");
        }catch (Exception ex){
            ex.printStackTrace();
            LOGGER.error("Error: {}", ex.getMessage(),ex);
            return "ERROR";
        }
    }

    //internet banking registration
    @RequestMapping(value = {"selfService"}, method = RequestMethod.POST, produces = "application/xml")
    public String iBankRegistration(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = "-1";
        try {
            response = webserviceRepo.ibankRegistration(payLoad); //
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        LOGGER.info("REQUEST FROM GATEWAY FOR REGISTRATION: {}\nRESPONSE TO GATEWAY: {}", payLoad, response);
        return response;
    }


    //PROCESS MT202 INCOMING
    @RequestMapping(value = {"processMT202Incoming"}, method = RequestMethod.POST, produces = "application/xml")
    public String processMT202Incoming(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
//        String response = SwiftService.processMT202Incoming(payLoad);//
        LOGGER.info("202 MESSAGE RECEIVED FROM SWIFT: {}", payLoad);
        String reference = payLoad.split("\\^")[0];
        if (payLoad.contains("{451:0}}")) {
            payLoad = payLoad.split("\\{451:0}}")[1];
        }
        if (payLoad.contains("{451:1}}")) {
            payLoad = payLoad.split("\\{451:1}}")[1];
        }
        String swiftMessage = payLoad;
        String message = swiftMessage + "|" + reference;
        swiftRepository.processBoTRTGSIncoming(message, "SWIFT");
        //queueProducer.sendToQueueMT202IncomingMessage(payLoad);
        return "<mt202Response><responseCode>0</responseCode><message>File received successfully</message></mt202Response>";
    }

    //internet banking registration
    @RequestMapping(value = {"reprocessChargeSplitting"}, method = RequestMethod.POST, produces = "application/xml")
    public String reprocessChargeSplitting(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String reference = XMLParserService.getDomTagText("batchReference", payLoad);
        String response = eftRepo.reprocessChargeSpliting(reference);//
        LOGGER.info("CHARGE SPLITING REQUEST: {}\nCHARGE SPLITTING RESPONSE: {}", payLoad, response);
        return response;
    }

    //PSSSF Monthly payroll payments
    @RequestMapping(value = {"psssfMonthlyPayrollPayment"}, method = RequestMethod.POST, produces = "application/json")
    public String PSSSFMonthlyPayrollPayments(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = pensionRepo.receivePayrollPaymentData(payLoad);
        LOGGER.info("PENSION PAYROLL DATA : {}\nPENSION PAYROLL DATA RESPONSE : {}", payLoad, response);
        return response;
    }
    //PSSSF Monthly payroll payments
    @RequestMapping(value = {"essMonthlyLoanRepayments"}, method = RequestMethod.POST, produces = "application/json")
    public String essMonthlyLoanRepayments(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = pensionRepo.receivePayrollPaymentData(payLoad);
        LOGGER.info("PENSION PAYROLL DATA : {}\nPENSION PAYROLL DATA RESPONSE : {}", payLoad, response);
        return response;
    }

    //TIPS CALLBACK
    @RequestMapping(value = {"tipsPaymentCallBackUrl"}, method = RequestMethod.POST, produces = "application/json")
    public String tipsCallBackUrl(@RequestParam Map<String, String> customeQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = tipsRepository.processTipsPaymentsFromCallBack(payLoad);//
        LOGGER.info("TIPS PAYMENT DATA CALL BACK PAYLOAD : {}\n TIPS PAYMENT DATA RESPONSE : {}", payLoad, response);
        return response;
    }


    @RequestMapping(value = {"mkobaTqsUpdate"}, method = RequestMethod.POST, produces = "application/xml")
    public String mkobaTqsUpdate(@RequestParam Map<String, String> customeQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = "-1";
        try {
            response = webserviceRepo.mkobaTqsUpdate(payLoad); //
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        LOGGER.info("REQUEST FROM MKOBA GATEWAY FOR TQS: {}\nRESPONSE TO GATEWAY: {}", payLoad, response);
        return response;
    }

    @RequestMapping(value = {"parseIso20022Message"}, method = RequestMethod.POST, produces = "application/xml")
    public String parseIso20022Message(@RequestParam Map<String, String> customeQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = "OK";
        try {
            eftRepo.parseIso20022Message(payLoad); //
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        LOGGER.info("Process incoming EFT transaction: {}", payLoad);
        return response;
    }


    /**
     * Start of visa card instant issuance
     *
     * @param customeQuery
     * @param payLoad
     * @param res
     * @return
     * @throws IOException
     */
    @RequestMapping(value = {"decrptBcx"}, method = RequestMethod.POST, produces = "application/xml")
    public String decrptBcx(@RequestParam Map<String, String> customeQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) throws IOException {
        String response = "OK";
        try {
            response = bcxService.createPANInitialPINBCX_tes(payLoad);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        LOGGER.info("BCX Decrypted: {} => {}", payLoad, response);
        return response;
    }


    @RequestMapping(value = "/linkCard", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UbxResponse> linkCard(@RequestBody CustomerInfoReq request) {
        LOGGER.info("Link Card Request: {}", request);
        try {
            // Populate additional request details

            UbxResponse result = bcxService.linkCardToAccount(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            LOGGER.error("Error processing linkCard request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UbxResponse(e.getMessage(),"96"));
        }
    }
    @RequestMapping(value = "/startLinkCard", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UbxResponse> startLinkingCard(@RequestBody CustomerInfoReq request) {
        LOGGER.info("Link Card Request: {}", request);try {
            kafkaTemplate.send(systemVariables.KAFKA_TOPIC_LINK_CARD_TOPIC,request);
            LOGGER.info("Card Issuance request Submitted");
            return ResponseEntity.ok(new UbxResponse("Card Issuance request Submitted","00"));

        } catch (Exception e) {
            LOGGER.error("Error processing linkCard request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UbxResponse(e.getMessage(),"96"));
        }
    }

    @RequestMapping(value = "/getCardAccount", method = RequestMethod.POST, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountNameQueryResp> getAccountFromCard(@RequestBody String pan) {
        LOGGER.info("Get account from card request: {}", pan);
        try {
            Map<String, Object> map = webserviceRepo.getCardAccount(pan);
            AccountNameQueryResp result = new AccountNameQueryResp();
            result.setResponseCode("0");
            result.setResponseMessage("Success");
            result.setAccountNo((String) map.get("account_no"));
            result.setAccountName((String) map.get("customer_name"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("Error getting account from card: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AccountNameQueryResp(e.getMessage(),"96"));
        }
    }

    //end of ubx visa card

    @RequestMapping(value = "activateCard", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UbxResponse> activateCard(@RequestBody UnblockCardRequest request) {
        Map<String, Object> response = new HashMap<>();
        LOGGER.info("Activate Card Request: {}", request);
        try {
            UbxResponse result = bcxService.activateCard(request);

            return ResponseEntity.ok(result);

        }catch (Exception e){
            LOGGER.info("Activate Card Error Response: {}", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UbxResponse(e.getMessage(),"96"));
        }
    }

    @RequestMapping(value = "reissueCardPin", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UbxResponse> reissueCardPin(@RequestBody ReissueCardPinRequest request) throws JsonProcessingException {
        LOGGER.info("REISSUE CARD REQUEST: {}", mapper.writeValueAsString(request));
        Map<String, Object> response = new HashMap<>();
        try {
            UbxResponse result = bcxService.reissueCardPin(request);
            LOGGER.info("Reissue Card Response: {}", result);
            return ResponseEntity.ok(result);

        }catch (Exception e){
            LOGGER.error("Error during Sending Card activation request: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UbxResponse(e.getMessage(),"96"));
        }

    }
    @RequestMapping(value = "changeCardPin", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UbxResponse> changeCardPin(@RequestBody PinChangeRequest request){
        LOGGER.info("Reissue Card Request: {}", request);
        try {
            UbxResponse result = bcxService.pinChange(request);
            LOGGER.info("PIN CHANGE RESPONSE: {}", mapper.writeValueAsString(result) );
            return ResponseEntity.ok(result);

        }catch (Exception e){
            LOGGER.error("Error during Sending Card activation request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new UbxResponse(e.getMessage(),"96"));
        }

    }


    //    @ApiOperation(value = "eftBatchCallback", notes = "")
//    @ApiResponses(value = {
//        @ApiResponse(code = 0, message = "Success"),
//        @ApiResponse(code = 99, message = "General Error"),})
    @PostMapping(value = {"eftBatchPaymentCallback"}, consumes = {"application/json", "application/xml;charset=UTF-8"}, produces = {"application/xml;charset=UTF-8", "application/json;charset=UTF-8"})
    public ResponseEntity<?> loanRepaymentCallback(@ApiParam(value = "Eft batch payment callback", required = true) @RequestBody(required = false) HashMap<String, String> reqBody) {
//        PortifolioPositionReportResp resp = loanManagementRepository.processCorebankingCallback(reqBody);
//        LOGGER.info("Loan Repayment Req: {}\nResp:{}", reqBody, resp);
//        https://172.25.29.80:9019/esb-ubx/abl/card/request/service/cardBlocking
        return ResponseEntity.ok().body(eftRepo.processCorebankingCallback(reqBody));
    }

    @ApiImplicitParams({
        @ApiImplicitParam(name = "batchRef", value = "batchRef", required = false, allowEmptyValue = false, paramType = "header")})
    @PostMapping(value = {"reprocessEftInwardBatch"}, consumes = {"application/json", "application/xml;charset=UTF-8"}, produces = {"application/xml;charset=UTF-8", "application/json;charset=UTF-8"})
    public ResponseEntity<?> processEftInwardWithIssues(@ApiParam(value = "Reprocess eft inward batch with errors", required = true) @RequestBody(required = false) HashMap<String, String> reqBody, HttpServletRequest req) {
        return ResponseEntity.ok().body(eftRepo.validateEftInwardBatch(req.getHeader("batchRef")));
    }


    /**PENSIONERS VERIFICATIONS**/
    @RequestMapping(value = {"pensionersLoanVerificationCallBack"}, method = RequestMethod.POST, produces = "application/json")
    public String pensionersLoanVerificationCallBack(@RequestBody(required = false) String payLoad) throws JsonProcessingException {
        LOGGER.info("Received Pensioner Callback Response payload ... {}", payLoad);
        String response = creditRepo.processPensionerCallBack(payLoad);
        return response;
    }

//    @PostMapping(value = {"processPayrollSalaries"}, consumes = {"application/json", "application/xml;charset=UTF-8"}, produces = {"application/xml;charset=UTF-8", "application/json;charset=UTF-8"})
//    public ResponseEntity<?> processStaffMonthlySalary(@RequestBody(required = false) String  payload) {
//        LOGGER.info("Checking the salary received ... {}", payload);
//        PAYROLL PAYROLL = XMLParserService.jaxbXMLToObject(payload, PAYROLL.class);
//        LOGGER.info("final mapped mshahara transactions ... {}", PAYROLL);
//        return ResponseEntity.ok().body(salaryProcessingRepository.insertPayrollSalary(PAYROLL));
//    }
    @PostMapping(value = {"processPayrollSalaries"},produces = {"application/xml;charset=UTF-8"})
    public ResponseEntity<?> processStaffMonthlySalary(@RequestBody(required = false) String  payload) {
        LOGGER.info("Checking the salary received ... {}", payload);
        PAYROLL PAYROLL = XMLParserService.jaxbXMLToObject(payload, PAYROLL.class);
        LOGGER.info("final mapped mshahara transactions ... {}", PAYROLL);
        return ResponseEntity.ok().body(salaryProcessingRepository.insertPayrollSalary(PAYROLL));
    }

    @PostMapping(value = {"processPayrollCallback"}, consumes = {"application/json", "application/xml;charset=UTF-8"}, produces = {"application/xml;charset=UTF-8", "application/json;charset=UTF-8"})
    public void processPayrollSalaryCallback(@RequestBody(required = false) String  payload) {
        LOGGER.info("Check callback received for payroll processing ... {}", payload);
        salaryProcessingRepository.processPayrollCallback(payload);
    }

    @PostMapping(value = {"verifyPensioner"}, consumes = {"application/x-www-form-urlencoded;charset=UTF-8"})
    public String verifyPensioner(@RequestParam Map<String, String> params) {
        String nin = params.get("nin");
        String accountNumber = params.get("accountNumber");
        String firstName = params.get("firstName");
        String middleName = params.get("middleName");
        String lastName = params.get("lastName");
        String accountStatus = params.get("accountStatus");
        String accountStatusDesc = params.get("accountStatusDesc");
        String fingerImage = params.get("fingerImage");
        String fingerCode = params.get("fingerCode");
        String phoneNumber = params.get("phoneNumber");
        LOGGER.info("Verify pensioner... {} {}", nin, firstName + " " + middleName + " " + lastName);
        return creditRepo.verifyPensioner(nin, accountNumber, firstName, middleName, lastName, accountStatus,
                accountStatusDesc, fingerImage, fingerCode, phoneNumber);
    }

    @PostMapping(value = {"saveEmkopoData"}, consumes = {"application/json", "application/xml;charset=UTF-8"}, produces = {"application/xml;charset=UTF-8", "application/json;charset=UTF-8"})
    public ResponseEntity<APIResponse<Transfers>> saveEmkopoTransactions(@RequestBody EMkopoReq eMkopoReq){
        return transferService.saveEmkopoData(eMkopoReq);
    }

    //Get customer loans
    @RequestMapping(value = {"customerLoans"}, method = RequestMethod.POST, consumes = "application/xml;charset=UTF-8", produces = "application/json")
    public ResponseEntity<APIResponse<List<Map<String, Object>>>> customerLoans(@RequestBody(required = false) String payload) throws IOException {
        try {
            List<Map<String, Object>> accounts = webserviceRepo.getCustomerLoanAccountsByRIM(payload);
            LOGGER.info("REQUEST FOR CUSTOMER LOANS: {}\nRESPONSE: {}", payload, accounts);
            return ResponseEntity.ok(new APIResponse<>(true, "Success", accounts));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new APIResponse<>(false, "Internal Server Error", null));
    }
//    @PostMapping("/instant/card/{id}/recomplete")
//    @PreAuthorize("hasAuthority('/fireCardIssue')")
//    public APIResponse<String> recomplete(@PathVariable Long id, Authentication auth) {
//        var resp = bcxService.processInstantIssuance(id);
//        String msg = resp.getResponseMessage() != null ? resp.getResponseMessage() : "Triggered";
//        return "00".equalsIgnoreCase(resp.getResponseCode())
//                ? APIResponse.ok(msg)
//                : new APIResponse<>(false, msg, null);
//    }
    @GetMapping("/instant/card/{id}/events")
    @PreAuthorize("hasAuthority('/fireCardIssue')")
    public APIResponse<List<CardEventDTO>> getEvents(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    ) {
        List<CardActionStatus> rows;
        if (since != null) {
            // If you have updatedAt column (you do), filter in memory or add a custom repo method if large
            rows = cardActionStatusRepository.findByCardIdOrderByUpdatedAtDesc(id)
                    .stream()
                    .filter(a -> a.getUpdatedAt() != null && a.getUpdatedAt().isAfter(since))
                    .collect(Collectors.toList());
        } else {
            rows = cardActionStatusRepository.findByCardIdOrderByUpdatedAtDesc(id);
        }
        List<CardEventDTO> out = rows.stream().map(CardEventDTO::from).collect(Collectors.toList());
        return APIResponse.ok(out);
    }

    @PostMapping("/instant/card/list")
    public APIResponse<List<InstantCardSummary>> listInstantCards(
            @RequestParam(name = "status",   required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "branch",   required = false) String branchParam,
            HttpSession session
    ) {
        // Resolve branch: prefer request param, else session, else no filter
        String branchFromSession = null;
        if (session != null) {
            Object v = session.getAttribute("branchCode");
            if (v != null) branchFromSession = String.valueOf(v);
        }
        String effectiveBranch = (branchParam != null && !branchParam.trim().isEmpty())
                ? branchParam.trim()
                : (branchFromSession != null ? branchFromSession : "0");

        // Build query spec
        Specification<CardDetailsEntity> spec = Specification
                .where(CardSpecs.createdBetween(fromDate, toDate))
                .and(CardSpecs.hasStatus(status))                // supports F/L/PR/R/C/PC/A/ALL
                .and(CardSpecs.originatingBranchIs(effectiveBranch)); // "0" or null => no branch filter

        List<CardDetailsEntity> cards =
                cardDetailsRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdDt"));

        List<InstantCardSummary> out = cards.stream()
                .map(Mapper::toSummary)
                .collect(Collectors.toList());

        return APIResponse.ok(out);
    }


    // === /instant/card/{id} ===
    @GetMapping("/instant/card/{id}")
    public APIResponse<InstantCardDetails> instantCardDetails(@PathVariable("id") Long id) {
        CardDetailsEntity c = cardDetailsRepository.findById(id).orElseThrow(NoSuchElementException::new);
        List<CardActionStatus> actions = cardActionStatusRepository.findByCardIdOrderByUpdatedAtDesc(id); // newest first
        List<EventTrail> events = actions.stream().map(Mapper::toEventDTO).collect(Collectors.toList());
        InstantCardDetails dto = new InstantCardDetails();
        dto.setCard(toSummary(c));
        dto.setEvents(events);
        return APIResponse.ok(dto);
    }

    //Get customer loan accounts
    @RequestMapping(value = {"loanAccountSummary"}, method = RequestMethod.POST, consumes = "application/xml;charset=UTF-8", produces = "application/json")
    public ResponseEntity<APIResponse<Map<String, Object>>> loanAccountSummary(@RequestBody(required = false) String payload) throws IOException {
        try {
            Map<String, Object> summary = webserviceRepo.getLoanAccountSummary(payload);
            LOGGER.info("REQUEST FOR LOAN ACCOUNT SUMMARY: {}\nRESPONSE: {}", payload, summary);
            return ResponseEntity.ok(new APIResponse<>(true, "Success", summary));
        } catch (Exception ex) {
            LOGGER.info("Error: {}", ex.getMessage());
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new APIResponse<>(false, "Internal Server Error", null));
    }

    @PostMapping(
            value = {"checkForTransaction"},
            consumes = {"application/json", "application/xml;charset=UTF-8"},
            produces = {"application/xml;charset=UTF-8", "application/json;charset=UTF-8"}
    )
    public ResponseEntity<APIResponse<Transfers>> checkForTransaction(@RequestBody TransactionCheckReq transactionCheckReq) {
        Optional<Transfers> optionalTransfer = transfersRepository.findByReference(transactionCheckReq.getReference());

        if (optionalTransfer.isPresent()) {
            Transfers transfer = optionalTransfer.get();

            if (transfer.getAmount().compareTo(transactionCheckReq.getAmount()) == 0) {
                return ResponseEntity.ok(new APIResponse<>(true, "Success", transfer));
            } else {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new APIResponse<>(false, "Amount does not match", null));
            }
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(false, "Internal Server Error", null));
        }
    }

    @PostMapping(
            value = "getBrinjalTerminalUsers"
    )
    @ResponseBody
    public String getTerminalsAjax(
            @RequestParam Map<String, String> customerQuery,
            TerminalUsersRequest request,
            HttpSession session
    ) {

        LOGGER.info("====<<<>>>>> {}", customerQuery);

        customerQuery.forEach((k, v) -> LOGGER.info("Param: {} = {}", k, v));


        String draw = customerQuery.get("draw");
        String fromDate = customerQuery.get("createdDate") + " " + customerQuery.get("fromTime") + ":00";
        String toDate = customerQuery.get("createdDate") + " " + customerQuery.get("toTime") + ":59";
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columnName");
        String columnSortOrder = customerQuery.get("sortOrder");
        String locked = customerQuery.get("locked");
        String blocked = customerQuery.get("blocked");

        return this.settingsRepo.getListTerminals(
                locked,
                blocked,
                fromDate,
                toDate,
                draw,
                start,
                rowPerPage,
                searchValue,
                columnIndex,
                columnName,
                columnSortOrder
        );
    }

    @GetMapping("/failedCards")
    public ResponseEntity<ResponseDTO<?>> getFailedCards(){
        return ResponseEntity.ok(bcxService.getAllFailedCards());
    }


    @RequestMapping(value = {"tissVpnTransferAdvice"}, method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> tissVpnTransferAdvice(@RequestParam Map<String, String> customerQuery, @RequestBody(required = false) String payLoad, HttpServletResponse res) {
        LOGGER.info("REQUEST FROM TISS VPN SWIFT<->KPRINTER: {}", payLoad);
        try {
            String lastPrimaryId = customerQuery.get("lastPrimaryId");
            return ResponseEntity.ok(swiftRepository.tissVpnTransferAdviceWork(lastPrimaryId));
        }catch (Exception ex){
            LOGGER.error("Error: {}", ex.getMessage(),ex);
            return null;
        }
    }

}
