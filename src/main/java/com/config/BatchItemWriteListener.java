package com.config;

import com.entities.PensionTransaction;
import com.entities.QueryBatchResponse;
import com.entities.Txn;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.repository.EftRepo;
import com.service.HttpClientService;
import com.service.XMLParserService;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BatchItemWriteListener implements ItemWriteListener<PensionTransaction> {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    SYSENV sysenv;

    @Autowired
    ObjectMapper jacksonMapper;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EftRepo.class);

    BigDecimal totalAmount = BigDecimal.ZERO;

    String batchRef = "";

    @Override
    public void beforeWrite(List<? extends PensionTransaction> list) {
        System.out.println("ItemWriteListener - beforeWrite");
        String batchRef = "";
        double amount = 0.0;
        int n = list.size();
        int startRecId = 0;
        int endRecId = 0;
        for (int i = 0; i < n; i++) {
            PensionTransaction pensionTransaction = list.get(i);
            if (i == 0) {
                batchRef = pensionTransaction.getBatchReference();
                startRecId = Integer.parseInt(pensionTransaction.getId());
            }
            if (i == n - 1) {
                endRecId = Integer.parseInt(pensionTransaction.getId());
            }
            amount += pensionTransaction.getAmount();
        }
        jdbcTemplate.update("INSERT INTO tmp_batch_transaction (reference, callbackUrl, createDt, endRecId, itemCount," +
                        " startRecId, `timestamp`, totalAmount, result, updateDt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                batchRef, sysenv.LOCAL_CALLBACK_URL + "/api/batchCallback",
                DateUtil.now("yyyy-MM-dd HH:mm:ss"), endRecId, list.size(), startRecId, DateUtil.now(), amount,
                "", null);
    }

    @Override
    public void afterWrite(List<? extends PensionTransaction> list) {
        System.out.println("ItemWriteListener - afterWrite");
//        totalAmount = new BigDecimal(0);
//        List<Txn> txns = list.stream().map(m -> {
//            if (batchRef.isEmpty()) {
//                batchRef = m.getBatchReference();
//            }
//            totalAmount = totalAmount.add(BigDecimal.valueOf(m.getAmount()));
//            Txn txn = new Txn();
//            txn.setAmount(String.valueOf(m.getAmount()));
//            txn.setTxnRef(String.valueOf(m.getBankReference()));
//            txn.setCurrency(String.valueOf(m.getCurrency()));
//            txn.setBatch(String.valueOf(m.getBatchReference()));
//            txn.setResult("");
//            txn.setBuId(String.valueOf(m.getBranchId()));
//            txn.setCrAcct(String.valueOf(m.getAccount()));
//            txn.setDrAcct(String.valueOf(m.getDrAccount()));
//            txn.setCreateDt(String.valueOf(m.getCreateDt()));
//            txn.setModule(String.valueOf(m.getModule()));
//            txn.setNarration(String.valueOf(m.getDescription()));
//            txn.setRecId(String.valueOf(m.getTrackingNo()));
//            txn.setRecSt(String.valueOf(m.getStatus()));
//            txn.setReverse(String.valueOf(m.getReverse()));
//            txn.setScheme("?");
//            txn.setChannel(String.valueOf(m.getChannelIdentifier()));
//            txn.setTries(String.valueOf(m.getTries()));
//            return txn;
//        }).collect(Collectors.toList());
//        QueryBatchResponse queryBatchResponse = new QueryBatchResponse();
//        StringBuilder request = new StringBuilder("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
//                "   <soapenv:Header/>\n" +
//                "   <soapenv:Body>\n" +
//                "      <api:processBatch>\n" +
//                "         <request>\n" +
//                "            <reference>" + batchRef + "</reference>\n" +
//                "            <itemCount>" + list.size() + "</itemCount>\n" +
//                "            <totalAmount>" + totalAmount + "</totalAmount>\n" +
//                "            <callbackUrl>" + sysenv.LOCAL_CALLBACK_URL + "/api/batchCallback</callbackUrl>\n" +
//                "            <txns>\n");
//        for (Txn txn: txns) {
//            request.append("<txn>\n" + "<amount>").append(txn.getAmount()).append("</amount>\n")
//                    .append("<batch>").append(txn.getBatch()).append("</batch>\n").append("<buId>")
//                    .append(txn.getBuId()).append("</buId>\n").append("<charge>").append(txn.getCharge())
//                    .append("</charge>\n").append("<chgId>").append(txn.getChgId()).append("</chgId>\n")
//                    .append("<code>").append(txn.getCode()).append("</code>\n").append("<crAcct>")
//                    .append(txn.getCrAcct()).append("</crAcct>\n").append("<createDt>").append(txn.getCreateDt())
//                    .append("</createDt>\n").append("<currency>").append(txn.getCurrency()).append("</currency>\n")
//                    .append("<drAcct>").append(txn.getDrAcct()).append("</drAcct>\n").append("<millis>")
//                    .append(txn.getMillis()).append("</millis>\n").append("<module>").append(txn.getModule())
//                    .append("</module>\n").append("<narration>").append(txn.getNarration()).append("</narration>\n")
//                    .append("<recId>").append(txn.getRecId()).append("</recId>\n").append("<recSt>")
//                    .append(txn.getRecSt()).append("</recSt>\n").append("<reqRef>").append(txn.getReqRef())
//                    .append("</reqRef>\n").append("<result>").append(txn.getResult()).append("</result>\n")
//                    .append("<reverse>").append(txn.getReverse()).append("</reverse>\n").append("<scheme>")
//                    .append(txn.getScheme()).append("</scheme>\n").append("<tries>").append(txn.getTries())
//                    .append("</tries>\n").append("<txnId>").append(txn.getTxnId()).append("</txnId>\n")
//                    .append("<txnRef>").append(txn.getTxnRef()).append("</txnRef>\n").append("</txn>\n");
//        }
//        request.append("</txns>\n" + "</request>\n" + "</api:processBatch>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>");
//        request = new StringBuilder(request.toString().replace("\n", ""));
//        //send http request to wsdl server
//        String soapResponse = HttpClientService.sendXMLReqBasicAuth(request.toString(), sysenv.CHANNEL_MANAGER_API_URL, "xapi", "x@pi#81*");
//        if (soapResponse.contains("return")) {
//            //parser soap xml to get clean xml
//            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
//            //byte data to string xml
//            String sxml = XMLParserService.xmlsrToString(xmlr);
//            LOGGER.info("Final response xml... {}", sxml);
//            //convert xml to java object
//            queryBatchResponse.setResult(XMLParserService.getDomTagText("result", sxml));
//            queryBatchResponse.setReference(XMLParserService.getDomTagText("reference", sxml));
//            queryBatchResponse.setMessage(XMLParserService.getDomTagText("message", sxml));
//            queryBatchResponse.setTxnId(XMLParserService.getDomTagText("txnId", sxml));
//        }
//        try {
//            LOGGER.info(jacksonMapper.writeValueAsString(queryBatchResponse));
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onWriteError(Exception e, @Nonnull List<? extends PensionTransaction> list) {
        System.out.println("ItemWriteListener - onWriteError");
        e.printStackTrace();
    }
}
