package com.controller.visaCardReport;

import com.helper.DateUtil;
import com.repository.EbankingRepo;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class VisaCardReportController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VisaCardReportController.class);

    @Autowired EbankingRepo ebankingRepo;
    @Autowired VisaCardReportRepo visaCardReportRepo;

    @RequestMapping("/fireVisaCardReport")
    public String visaReport(Model model,HttpSession session, @RequestParam Map<String, String> customeQuery){
        model.addAttribute("startDate", DateUtil.previosDay(10));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("hqCheck",session.getAttribute("branchCode"));
        return "/maitools/visaCardReport";
    }

    @RequestMapping(value = "/fireVisaCardReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public VisaCardJsonResponse getGePGTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        VisaCardJsonResponse visaCardJsonResponse = new VisaCardJsonResponse();
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        String branchCode = customeQuery.get("branchCode");
        String statusCode = customeQuery.get("statusCode");

        List<Map<String,Object>> response = visaCardReportRepo.getVisaReportAjax(branchCode,statusCode, fromDate, toDate);
//        LOGGER.info("The final response for Visa Card Report search is: {}", response);
        visaCardJsonResponse.setStatus("SUCCESS");
        visaCardJsonResponse.setResult(response);

        return visaCardJsonResponse;
   }
}
