package com.service;

import com.config.SYSENV;
import com.dao.sms.SMSDTO;
import com.dao.kyc.response.ors.ResponseDTO;
import com.dao.sms.SMSResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@Slf4j
@AllArgsConstructor
public class SMSServiceImpl implements SMSService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SYSENV env;
    @Override
    public ResponseDTO<?> sendSMS(SMSDTO smsDTO) {
        return send(smsDTO.getMessage(),smsDTO.getPhone(),generateISOTransactionReference(), env.TCB_SMS_NOTIFICATION_URI);
    }

    @Override
    public String sendXMLRequest(String requestXML, String url) {
        RestTemplate restTemplate = new RestTemplate();
        String responseBody = "-1";

        log.info("=====PAYLOAD=====: {}", requestXML);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);

            HttpEntity<String> entity = new HttpEntity<>(requestXML, headers);

            log.info("Sending request to URL: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            responseBody = extractJsonFromXml(response.getBody());
            log.info("Response code[{}]: {}", response.getStatusCode(), responseBody);

        } catch (HttpStatusCodeException e) {
            log.warn("HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            responseBody = e.getResponseBodyAsString();
        } catch (Exception ex) {
            log.error("Exception occurred while sending XML request to {}: {}", url, ex.getMessage());
        }

        return responseBody;
    }


    private ResponseDTO<?> send(String smsBody, String smsIdn, String sessionId, String smsGateAwayUrl) {
        ResponseDTO<String> result = new ResponseDTO<>();
        try {
            String smsToSend = smsBody.replace("&", "and").replace("'", "");

            String xmlMsg = "<methodCall>" +
                    "<methodName>TPB.MOBSMS</methodName>" +
                    "<params>" +
                    "<param><value><string>" + smsIdn + "</string></value></param>" +
                    "<param><value><string>" + smsToSend + "</string></value></param>" +
                    "<param><value><string>" + sessionId + "</string></value></param>" +
                    "</params>" +
                    "</methodCall>";

            String jsonResponse = sendXMLRequest(xmlMsg, smsGateAwayUrl);
            log.info("=========SMS RESPONSE BODY ****: {}", jsonResponse);

            SMSResponse smsRes = objectMapper.readValue(jsonResponse, SMSResponse.class);

            if ("SUCCESS".equalsIgnoreCase(smsRes.getStatus())) {
                result.setResultCode(200);
                result.setSuccess(true);
                result.setMessage(smsRes.getDescription());
                result.setData(smsRes.getDescription());
            } else {
                result.setResultCode(203);
                result.setMessage(smsRes.getStatus()!=null ? smsRes.getStatus() + " "+ smsRes.getDescription(): "Unknown Error");
                result.setData(smsRes.getDescription()!=null?smsRes.getDescription():"Unknown Error");
            }

        } catch (Exception e) {
            log.error("Error sending SMS: ", e);
            result.setResultCode(500);
            result.setMessage("Error sending SMS: " + e.getMessage());
            result.setData(e.getMessage());
        }

        return result;
    }
    private  String generateISOTransactionReference() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomNumber = 1000 + random.nextInt(9000);
        return timestamp + randomNumber;
    }
    private String extractJsonFromXml(String xml) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes()));

            NodeList nodes = doc.getElementsByTagName("string");
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent();
            } else {
                log.warn("No <string> tag found in response");
            }
        } catch (Exception e) {
            log.error("Failed to parse XML response: {}", e.getMessage());
        }

        return "-1";
    }
}
