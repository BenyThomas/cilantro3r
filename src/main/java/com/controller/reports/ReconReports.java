/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller.reports;

import com.controller.Recon;
import com.helper.DateUtil;
import com.repository.Recon_M;
import com.repository.reports.ReconReportsRepo;
import com.service.JasperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class ReconReports {

    @Autowired
    JasperService jasperService;
    @Autowired
    ReconReportsRepo reconReportsRepo;


    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReconReportsRepo.class);

//    //STANDARD RECONCILIATION REPORT
//       @RequestMapping(value = {"/cummulativeReconciliationReport"}, method = {RequestMethod.GET})
//    @ResponseBody
//    public String cummulativeReconciliationReport(@RequestParam Map<String, String> customeQuery, HttpServletResponse res, HttpSession session) throws IOException {
//        String ttype = session.getAttribute("ttype").toString();
//        String reconDate = session.getAttribute("txndate").toString();
//        String printedBy = session.getAttribute("username").toString();
//        String txnType = "ATM_POS";
//        return reconReportsRepo.getReconSummaryReport(customeQuery.get("reportFormat"), res, DateUtil.now("yyyyMMddHHmm") + "ReconReport", txnType, ttype, reconDate, printedBy);
//    }


}
