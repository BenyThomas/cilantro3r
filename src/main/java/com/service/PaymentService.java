package com.service;

import com.config.SYSENV;
import com.helper.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Component
public class PaymentService {
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcTemplateCbs;
    @Autowired
    SYSENV systemVariable;
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    public void retryFailedCBSPayments() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = dateFormat.format(cal.getTime());
        List<Map<String, Object>> transactions = jdbcTemplateCbs.queryForList(
                "SELECT * FROM CHANNELMANAGER.PHL_QUEUE pq WHERE pq.REC_ST = 'Q' AND pq.TRIES = 5 AND CREATE_DT > DATE '"
                + date + "'"
        );
        for (Map<String, Object> transaction: transactions) {
            log.info("TRANSACTION WITH REF {}, BATCH {}, MODULE {}, ACCOUNT {}, AMOUNT {}, AND NARRATION {} HAS BEEN RETRIED",
                    transaction.get("TXN_REF"), transaction.get("BATCH"), transaction.get("MODULE"), transaction.get("CR_ACCT"),
                    transaction.get("AMOUNT"), transaction.get("NARRATION"));
            String req = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <api:reprocessBatch>\n" +
                    "         <!--Optional:-->\n" +
                    "         <request>\n" +
                    "            <reference>PEN" + DateUtil.now("yyyyMMddHHmmss") + "</reference>\n" +
                    "            <!--Optional:-->\n" +
                    "            <batch>" + transaction.get("BATCH") + "</batch>\n" +
                    "         </request>\n" +
                    "      </api:reprocessBatch>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String coreResponse = HttpClientService.sendXMLReqBasicAuth(req, this.systemVariable.CHANNEL_MANAGER_API_URL,
                    "xapi", "x@pi#81*");
            log.info("RAW RESPONSE FROM CORE BANKING: " + coreResponse);
            if (!coreResponse.equals("-1")) {
                XMLStreamReader reader = XMLParserService.createXMLStreamReaderFromSOAPMessage(coreResponse, "body", "return");
                String response = XMLParserService.xmlsrToString(reader).replace(
                        " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "")
                        .replace(" xsi:nil=\"true\"", "");
                log.info("RESPONSE FROM CORE BANKING: {}", response);
            }
        }
    }
}
