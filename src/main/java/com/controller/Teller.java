/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.GepgBillQry;
import com.core.MY_Controller;
import com.repository.TellerRepo;
import com.repository.TransactionRepo;
import com.service.FileService;
import com.service.HttpClientService;
import com.service.XMLParserService;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class Teller {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientService.class);
    @Autowired
    TransactionRepo transactionRepo;
    
    @Autowired
    TellerRepo tellerRepo;
    
    @Autowired
    FileService fileService;

    @RequestMapping(value = "/tellerPayments")
    public String tellerModule(Model model, HttpSession session) {
        model.addAttribute("modules",tellerRepo.getUserPaymentsModulesPerRole(session.getAttribute("roleId").toString()));
        return "modules/teller/tellerDashboard";
    }
    
    @RequestMapping(value = "/governmentPayments")
    public String governmentPayments(Model model, HttpSession session) {
        System.out.println("Malipo ya Serikali");
        return "modules/teller/governmentPayments";
    }
    
    @RequestMapping("/queryBill")
    public String gepgQryBillGet(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request) {
        Random rand = new Random();
        String reqId = String.valueOf(System.currentTimeMillis()).substring(3, 13) + "" + rand.nextInt(1000);;
        String controlNo = customeQuery.get("controlNo");
        LOGGER.info("partnerMgtGepgQryBillGet with Control No: {}", controlNo);
        LOGGER.info("partnerMgtGepgQryBillGet with req Id: {}", reqId);
        String xmlReq = "<request>"
                + "<COMMAND>BillQryReqProxy</COMMAND>"
                + "<CONTROL_NO>" + controlNo + "</CONTROL_NO>"
                + "<ID>" + reqId + "</ID>"
                + "</request>";
        LOGGER.info("partnerMgtGepgQryBillGet with req xml: {}", xmlReq);
        model.addAttribute("GEPG_RESP_CODE", "-1");
        String soapResponse = HttpClientService.sendXMLRequest(xmlReq, MY_Controller.PROXY_URL);
        if (soapResponse.equals("-1")) {
            model.addAttribute("ERROR_MESSAGE", "Oops!, We are experiencing a technical problem. Please contact your system administrator code: -1");
        } else {
            soapResponse = soapResponse.replace("&", "");
            soapResponse = soapResponse.replace("!", "").trim();
            LOGGER.info(XMLParserService.xmlToStringPretty(soapResponse, 3));
            //convert xml to java object
            GepgBillQry response = XMLParserService.jaxbXMLToObject(soapResponse, GepgBillQry.class);
            LOGGER.trace(response.toString());
            //7336 - for tiss
            model.addAttribute("GEPG_RESP_CODE", response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode() == null ? "-1" : response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode());
            if (response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode().equals("7101") || response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode().equals("7336")) {
                model.addAttribute("BillCtrNum", response.getGepgBillQryResp().getBillTrxDtl().getBillCtrNum());
                model.addAttribute("PyrName", response.getGepgBillQryResp().getBillTrxDtl().getPyrName());
                model.addAttribute("PyrCellNum", response.getGepgBillQryResp().getBillTrxDtl().getPyrCellNum());
                model.addAttribute("PyrEmailAddr", response.getGepgBillQryResp().getBillTrxDtl().getPyrEmailAddr());
                model.addAttribute("BillDesc", response.getGepgBillQryResp().getBillTrxDtl().getBillDesc());
                model.addAttribute("BillPayOpt", response.getGepgBillQryResp().getBillTrxDtl().getBillPayOpt());
                model.addAttribute("BillExprDt", response.getGepgBillQryResp().getBillTrxDtl().getBillExprDt());
                model.addAttribute("BillAmt", response.getGepgBillQryResp().getBillTrxDtl().getBillAmt());
                model.addAttribute("CCy", response.getGepgBillQryResp().getBillTrxDtl().getCCy());
                model.addAttribute("SpName", response.getGepgBillQryResp().getBillGrpHdr().getSpName());
                model.addAttribute("SpCode", response.getGepgBillQryResp().getBillGrpHdr().getSpCode());
                model.addAttribute("BillItems", response.getGepgBillQryResp().getBillTrxDtl().getBillItems().getBillItem());
                model.addAttribute("ERROR_MESSAGE", null);
            } else if (response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode().equals("7204")) {
                model.addAttribute("ERROR_MESSAGE", "Bill does not exist/ or has expired or already paid. code: " + response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode());
            } else if (response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode().equals("7242")) {
                model.addAttribute("ERROR_MESSAGE", "You are sending invalid request to gepg. Please contact your system administrator code: " + response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode());
            } else {
                model.addAttribute("ERROR_MESSAGE", response.getGepgBillQryResp().getBillGrpHdr().getBillStsCode().equals("7242"));
            }
        }
        return "modules/teller/GePGBillDetails";
    }
    
}
