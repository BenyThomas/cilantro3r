package com.helper;

import com.DTO.IBANK.PaymentReq;
import com.DTO.SOAPResponse;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.models.Transfers;
import com.repository.BanksRepo;
import com.repository.TransfersRepository;
import com.repository.WebserviceRepo;
import com.service.HttpClientService;
import com.service.XMLParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import philae.api.XaResponse;

import jakarta.transaction.Transactional;
import javax.xml.stream.XMLStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;


@Component
@Slf4j
public class TransactionUtil {

    @Value("${spring.profiles.active}")
    private String envPath;

    @Autowired
    private TransfersRepository transferRepository;

    @Autowired
    private BanksRepo banksRepo;

    @Autowired
    SYSENV systemVariable;

    @Autowired
    WebserviceRepo webserviceRepo;

    public int comfirmOutWardTransfer(Transfers txRequest){
        int status = -1;
        XaResponse xiResponse = null;

        String requestData = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ach=\"http://ach.PHilae/\">" +
                    "<soapenv:Header/>" +
                    "<soapenv:Body>" +
                        "<ach:confirmOutwardTransfer>" +
                            "<transfer>" +
                                "<reference>" + txRequest.getReference() + "</reference>" +
                                "<txnRef>" + txRequest.getReference() + "</txnRef>" +
                                "<createDate>" + txRequest.getCreateDt() +"</createDate>" +
                                "<employeeId>" + txRequest.getInitiatedBy() +"</employeeId>" +
                                "<supervisorId>" + txRequest.getInitiatedBy() +"</supervisorId>" +
                                "<transferType>" + txRequest.getTxn_type() +"</transferType>" +
                                "<currency>" + txRequest.getCurrency() +"</currency>" +
                                "<amount>" + txRequest.getAmount() +"</amount>" +
                                "<debitFxRate>" + "0" +"</debitFxRate>" +
                                "<creditFxRate>" + "0" +"</creditFxRate>" +
                                "<receiverBank>" + txRequest.getBeneficiaryBIC() +"</receiverBank>" +
                                "<receiverAccount>" + txRequest.getBeneficiaryBIC() +"</receiverAccount>" +
                                "<receiverName>" + txRequest.getBeneficiaryName() +"</receiverName>" +
                                "<senderBank>" + txRequest.getSenderBIC() +"</senderBank>" +
                                "<senderAccount>" + txRequest.getSenderBIC() +"</senderAccount>" +
                                "<senderName>" + txRequest.getSenderName() +"</senderName>" +
                                "<description>" + txRequest.getPurpose() +"</description>" +
                                "<scheme>" + "Test" +"</scheme>" +
                                "<txnId>" + txRequest.getTxid() +"</txnId>" +
                                "<contraAccount>" + "0000" +"</contraAccount>" +
                                "<reversal>" + "False" +"</reversal>" +
                                "<userRole>" +
                                    "<roleId>" + "2" +"</roleId>" +
                                    "<userId>" + "3" +"</userId>" +
                                    "<role>" + "3" +"</role>" +
                                    "<userName>" + "3" +"</userName>" +
                                    "<branchId>" + txRequest.getBranchNo() +"</branchId>" +
                                    "<buRoleId>" + "3" +"</buRoleId>" +
                                    "<userRoleId>" + "3" +"</userRoleId>" +
                                    "<supervisor>" + "3" +"</supervisor>" +
                                    "<branchCode>" + "3" +"</branchCode>" +
                                    "<branchName>" + "3" +"</branchName>" +
                                    "<limits>" +
                                        "<limit>" +
                                            "<creditLimit>" + "3" +"</creditLimit>" +
                                            "<currency>" + "3" +"</currency>" +
                                            "<debitLimit>" + "3" +"</debitLimit>" +
                                            "<role>" + "3" +"</role>" +
                                            "<roleId>" + "3" +"</roleId>" +
                                        "</limit>" +
                                    "</limits>" +
                                    "<drawers>" +
                                        "<drawer>" +
                                            "<drawerId>" + "3" +"</drawerId>" +
                                            "<drawerNumber>" + "3" +"</drawerNumber>" +
                                            "<drawerAccount>" + "3" +"</drawerAccount>" +
                                            "<openDate>" + "3" +"</openDate>" +
                                            "<status>" + "3" +"</status>" +
                                            "<currencies>" +
                                                "<currency>" +
                                                    "<code>" + "3" +"</code>" +
                                                    "<id>" + "3" +"</id>" +
                                                    "<name>" + "3" +"</name>" +
                                                    "<points>" + "3" +"</points>" +
                                                "</currency>" +
                                            "</currencies>" +
                                        "</drawer>" +
                                    "</drawers>" +
                                "</userRole>" +
                                "<valueDate>" + "0" +"</valueDate>" +
                            "</transfer>" +
                        "</ach:confirmOutwardTransfer>" +
                    "</soapenv:Body>" +
                "</soapenv:Envelope>";

        String reqUrl = "http://172.25.2.228:3800/philae/xws/tach?wsdl";
        String response = HttpClientService.sendXMLReqBasicAuth(requestData, reqUrl, "tach", "t@ch#71*");

        System.out.println("=======>>>>>> this response" + response);

        if(response != null && !response.equals("-1") && !response.isEmpty()){
            try{
                XMLStreamReader xmlReader = XMLParserService.createXMLStreamReaderFromSOAPMessage(response, "body", "return");
                String sxml = XMLParserService.xmlsrToString(xmlReader);

                xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);

                log.info("========>>><<<<>>><< XML Response {}", xiResponse);

                if (xiResponse != null) {
                    status = xiResponse.getResult();

                    if (status == 0) {
                        System.out.println("======>>>>>>true");

                        outWardTransferResponse(sxml);
                    }
                } else {
                    status = 96;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error parsing XML response", e);
            }
        }else {
            log.info("========<<<<>>>> Failed to read REspnse {}");
        }
        return status;
    };

    public int processEFTandRTGSTransactions() {
        int status = -1;
        XaResponse xiResponse = null;

        String reference = generateRandomRf(); // Generate once per transaction
        TransactionReq txn = new TransactionReq();
        txn.setReference(reference);
        txn.setTransferType("RTGS");

        String requestData = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ach=\"http://ach.PHilae/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<ach:queryOutwardTransfers>" +
                "<query>" +
                "<reference>" + txn.getReference() + "</reference>" +
                "<transferType>" + txn.getTransferType() + "</transferType>" +
                "</query>" +
                "</ach:queryOutwardTransfers>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        String reqUrl = "http://172.25.2.228:3800/philae/xws/tach?wsdl";
        String response = HttpClientService.sendXMLReqBasicAuth(requestData, reqUrl, "tach", "t@ch#71*");

        System.out.println("=====>>>>>????>>>>>><< response" + response);

        if (response != null && !response.equals("-1") && !response.isEmpty()) {
            try {
                XMLStreamReader xmlReader = XMLParserService.createXMLStreamReaderFromSOAPMessage(response, "body", "return");
                String sxml = XMLParserService.xmlsrToString(xmlReader);

                xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);

                if (xiResponse != null) {
                    status = xiResponse.getResult();

                    if (status == 0) {
                        handleResponse(sxml);
                    }
                } else {
                    status = 96;
                }

            } catch (Exception e) {
                throw new RuntimeException("Error parsing XML response", e);
            }
        } else {
            log.warn("Invalid or empty SOAP response.");
        }

        return status;
    }

    public String outWardTransferResponse(String response) throws JsonProcessingException{

        if(!response.contains("<message>Success</message>")){
            return "No processed Transfer";
        }

        return "Success Outward Transfer";
    }

    @Transactional
    public String handleResponse(String response) throws JsonProcessingException {
        if (!response.contains("<message>Success</message>")) {
            return "Transaction Not Found";
        }

        XmlMapper xmlMapper = new XmlMapper();
        SOAPResponse parsedResponse = xmlMapper.readValue(response, SOAPResponse.class);

        if (parsedResponse.getTransfers() == null || parsedResponse.getReference().isEmpty()) {
            return "No transfers found";
        }

        for (TransferItem item : parsedResponse.getTransfers()) {
            try {
                String txnId = item.getTxnId();
                System.out.println("===========item " + txnId);

                boolean existingTransfer = transferRepository.existsByTxid(txnId);
                String randomRef = generateRandomRf();

                if (existingTransfer) {
                    log.info("Transfer with generated reference {} already exists", randomRef);
                    continue;
                }

                if (!existingTransfer) {
                    Transfers transfer = new Transfers();
                    String contraAccount = item.getContraAccount();

                    String senderAddress = String.join("\n",
                            item.getSenderAddressLine1(),
                            item.getSenderAddressLine2(),
                            item.getSenderAddressLine3(),
                            item.getSenderAddressLine4());

                    String[] parts = contraAccount.split("-");
                    if (parts.length >= 2) {
                        transfer.setBranchNo(parts[1]);
                    }

                    String beneficiaryBic = item.getReceiverBank();
                    boolean isLocal = banksRepo.isLocalBank(beneficiaryBic.substring(0, 8));
                    String txnType = isLocal ? "001" : "004";

                    // Setup Payment Request
                    PaymentReq preq = new PaymentReq();
                    preq.setReference(randomRef);
                    preq.setType(txnType);
                    preq.setSenderAccount(item.getSenderAccount());
                    preq.setSenderName(item.getSenderName());
                    preq.setAmount(new BigDecimal(item.getAmount()));
                    preq.setCurrency(item.getCurrency());
                    preq.setBeneficiaryAccount(item.getReceiverBank());
                    preq.setBeneficiaryName(item.getReceiverName());
                    preq.setBeneficiaryBIC(item.getSenderBank());
                    preq.setSenderAddress(senderAddress);
                    preq.setBeneficiaryContact(contraAccount);
                    preq.setSenderPhone("phone");
                    preq.setDescription(item.getDescription());
                    preq.setIntermediaryBank(item.getSenderBank());
                    preq.setSpecialRateToken(item.getTxnId());
                    preq.setInitiatorId(item.getEmployeeId());
                    preq.setCustomerBranch(parts.length >= 2 ? parts[1] : null);
                    preq.setBatchReference(randomRef);
                    preq.setCorrespondentBic(item.getReceiverBank());
//                    preq.setCreateDt(LocalDateTime.now());

                    // Setup Transfer Entity
                    transfer.setCurrency(item.getCurrency());
                    transfer.setAmount(item.getAmount());
                    transfer.setSourceAcct(item.getSenderAccount());
                    transfer.setDestinationAcct(item.getReceiverAccount());
                    transfer.setBeneficiaryName(item.getReceiverName());
                    transfer.setBeneficiaryBIC(item.getReceiverBank());
                    transfer.setSenderName(item.getSenderName());
                    transfer.setSenderBIC(item.getSenderBank());
                    transfer.setTxid(txnId);
                    transfer.setComments(item.getDescription());
                    transfer.setPurpose(item.getDescription());
                    transfer.setReference(randomRef);
                    transfer.setSenderAddress(senderAddress);
                    transfer.setTxn_type(txnType);
                    transfer.setResponseCode("0");
                    transfer.setMessage("Success");
                    transfer.setInitiatedBy(item.getEmployeeId());

                    comfirmOutWardTransfer(transfer);
                    String respons = webserviceRepo.transferPaymentMakeSwiftMessage(preq, txnId);
                    System.out.println("==========>> Swift Message: " + respons);

                } else {
                    log.info("Transfer with txnId {} already exists", txnId);
                }
            } catch (Exception e) {
                log.error("Error processing transfer with txnId {}: {}", item.getTxnId(), e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return "Success: All Transfers processed";
    }


    public String generateRandomRf() {
        return "REF" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
}
