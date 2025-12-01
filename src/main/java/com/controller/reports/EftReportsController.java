/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller.reports;

import com.helper.DateUtil;
import com.repository.EftRepo;
import com.repository.reports.EftReportsRepo;
import com.repository.reports.ReconReportsRepo;
import com.service.JasperService;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class EftReportsController {

    /*
    *Preview EFT SUCCESS/FAILED TRANSACTIONS PER BANK
     */
    @Autowired
    EftRepo eftRepo;
    @Autowired
    JasperService jasperService;

    @Autowired
    ReconReportsRepo reconReportsRepo;

    @Autowired
    EftReportsRepo eftReportsRepo;

    @RequestMapping(value = "/eftReports")
    public String previewEftSuccessTxns(@RequestParam Map<String, String> customeQuery, Model model) {
        return "modules/eft/reports/outwardTransactionReport";
    }

    @RequestMapping(value = "/getEftReportsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getEftBulkPaymentsOnWorkFlowAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        Calendar cal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        cal.add(Calendar.DATE, -3);
        String date = dateFormat.format(cal.getTime());

        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return eftRepo.getEftBulkPaymentsOnWorkFlowAjax(date,(String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = {"/downloadEftOutwardReport"}, method = {RequestMethod.GET})
    @ResponseBody
    public String onlineMandateForm(@RequestParam Map<String, String> customeQuery, HttpServletResponse res) throws IOException {
        return eftReportsRepo.getEftOutwardTranstion(customeQuery.get("batchReference"), customeQuery.get("reportFormat"), res, "eftOutwardReports" + DateUtil.now("yyyy-MM-dd-HHmmss"));

    }
}
