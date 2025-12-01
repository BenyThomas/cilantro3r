package com.controller.itax.GePG;

import com.controller.itax.ItaxJsonResponse;
import com.helper.DateUtil;
import com.repository.itax.GePG.EgaRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class EgaController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EgaController.class);

    @Autowired
    EgaRepository egaRepository;

    @RequestMapping("/fireGePGTransactions")
    public String allGePGTransactions(Model model){
        model.addAttribute("startDate", DateUtil.previosDay(30));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        return "/itax/GePG/gepgTransactions";
    }

    @RequestMapping(value = "/fireGePGTransactionsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ItaxJsonResponse getGePGTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        ItaxJsonResponse itaxJsonResponse = new ItaxJsonResponse();
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        String txnStatus = customeQuery.get("txnStatus");

        List<Map<String,Object>> response = egaRepository.getGePGTransactions(txnStatus, fromDate, toDate);
        LOGGER.info("The final response for ega_transactions search is: {}", response);
        itaxJsonResponse.setStatus("SUCCESS");
        itaxJsonResponse.setResult(response);

        return itaxJsonResponse;
    }

}
