package com.controller.loan;

import com.DTO.GeneralJsonResponse;
import com.DTO.LoanRepayment;
import com.DTO.Reports.*;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.Mapper;
import com.repository.loan.LoanCreditRepository;
import com.service.HttpClientService;
import com.service.XMLParserService;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import philae.api.BnUser;
import philae.api.UsRole;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.helper.MaiString.cbsReference;

/**
 * @author daudi.kajilo
 */

@Controller
@Slf4j
public class LoanCreditController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoanCreditController.class);

    @Autowired
    LoanCreditRepository loanCreditRepository;

    @Autowired
    SYSENV sysenv;

    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private HttpClientService httpClientService;

    @GetMapping("/loanCreditDashboard")
    public String loanCreditDashboard(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","LOAN/CREDIT DASHBOARD");
        model.addAttribute("loanCreditPermissions", loanCreditRepository.getLoanModulePermissions("/loanCreditDashboard", httpSession.getAttribute("roleId").toString()));
        return "/modules/loan/loanCreditDashboard";
    }

    @GetMapping("/loanScoringEngine")
    public String loanScoringEngine(Model model, HttpSession httpSession) throws Exception {
        model.addAttribute("pageTitle","LOAN SCORING ENGINE");
        model.addAttribute("loanTypes",loanCreditRepository.getLoanTypes());
        return "/modules/loan/loanScoringEngine";
    }

    @PostMapping(value = "/loanScoringEngineAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loanScoringEngineAjax(@RequestParam Map<String,String> customeQuery){
        return loanCreditRepository.getScoringEngineAjax(customeQuery.get("loanType"));
    }

    @GetMapping("/overdraftScoredCustomerLimit")
    public String overdraftScoredCustomerLimit(Model model, HttpSession httpSession) throws Exception {
        model.addAttribute("pageTitle","SCORED CUSTOMER DETAILS");
        model.addAttribute("fromDate", DateUtil.previosDay(2));
        model.addAttribute("toDate",DateUtil.tomorrow());
        model.addAttribute("loanTypes",loanCreditRepository.getLoanTypes());
        return "/modules/loan/overdraftScoredCustomerLimit";
    }

    @PostMapping(value = "/overdraftScoredCustomerLimitAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String overdraftScoredCustomerLimitAjax(@RequestParam Map<String, String> customeQuery){
        return loanCreditRepository.getScoredCustomerLimit(customeQuery.get("loanType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
    }

    @GetMapping("/customersOtherLoans")
    public String customersOtherLoans(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","CUSTOMER OTHER LOANS");
        return "/modules/loan/customersOtherLoans";
    }
    @GetMapping("/customerScoring")
    public String customerScoring(Model model, HttpSession httpSession) throws Exception {
        model.addAttribute("pageTitle","CUSTOMER SCORING");
        model.addAttribute("loanTypes",loanCreditRepository.getLoanTypes());
        return "/modules/loan/customerScoring";
    }
    @GetMapping("/customerLoans")
    public String customerLoans(Model model, HttpSession httpSession) throws Exception {
        model.addAttribute("pageTitle","CUSTOMER LOANS");
        model.addAttribute("fromDate", DateUtil.previosDay(2));
        model.addAttribute("toDate",DateUtil.tomorrow());
        model.addAttribute("loanTypes",loanCreditRepository.getLoanTypes());
        return "/modules/loan/customerLoans";
    }

    @GetMapping("/customerPayroll")
    public String customerPayroll(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","CUSTOMERS PAYROLL");
        return "/modules/loan/customerPayroll";
    }

    @PostMapping(value="/customerLoansAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String customerLoansAjax(@RequestParam Map<String,String> customeQuery){
        return loanCreditRepository.customerLoansAjax(customeQuery.get("loanType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
    }
    @GetMapping("/customerRepayments")
    public String customerRepayments(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","CUSTOMER REPAYMENT");
        return "/modules/loan/customerRepayments";
    }

    @GetMapping("/loansReport")
    public String loansReport(Model model, HttpSession httpSession) throws Exception {
        model.addAttribute("pageTitle","LOANS REPORT");
        model.addAttribute("fromDate", DateUtil.previosDay(2));
        model.addAttribute("toDate",DateUtil.tomorrow());

        model.addAttribute("loanTypes",loanCreditRepository.getLoanTypes());
        return "/modules/loan/loansReport";
    }

    @PostMapping(value="/loansReportAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loansReportAjax(@RequestParam Map<String,String> customeQuery){
        return loanCreditRepository.loansReportAjax(customeQuery.get("loanType"),customeQuery.get("reportType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
    }

    @PostMapping(value="/fireOutstandingLoanAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String outstandingLoanAjax(@RequestParam Map<String,String> customeQuery){
        String response = loanCreditRepository.outstandingLoanAjax(customeQuery.get("loanType"),customeQuery.get("reportType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
        return response;
    }

    @PostMapping(value="/fireRepaymentSummaryPerAccountAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireRepaymentSummaryPerAccountAjax(@RequestParam Map<String,String> customeQuery){
        return loanCreditRepository.fireRepaymentSummaryPerAccountAjax(customeQuery.get("loanType"),customeQuery.get("reportType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
    }

    @PostMapping(value="/fireRepaymentSummaryPerLoanAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireRepaymentSummaryPerLoanAjax(@RequestParam Map<String,String> customeQuery){
        return loanCreditRepository.fireRepaymentSummaryPerLoanAjax(customeQuery.get("loanType"),customeQuery.get("reportType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
    }

    @RequestMapping(value="/firePreviewCustomerLoansAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String firePreviewCustomerLoansAjax(@RequestParam Map<String, String> customeQuery){
        return loanCreditRepository.previewCustomerLoansAjax(customeQuery.get("accountNumber"));
    }

    @RequestMapping(value="/fireGetDailLoanBalanceFromBrinjal", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireGetDailLoanBalanceFromBrinjal(@RequestParam Map<String, String> customeQuery){
        return loanCreditRepository.fireGetDailLoanBalanceFromBrinjal(customeQuery.get("loanType"),customeQuery.get("reportType"),customeQuery.get("fromDate"),customeQuery.get("toDate"));
    }

    @GetMapping("/loanRepayment")
    public String loanRepayment(Model model, HttpSession httpSession) throws Exception {
        model.addAttribute("pageTitle","MANUAL LOAN REPAYMENT MODULE");
        model.addAttribute("loanTypes",loanCreditRepository.getLoanTypes());
        model.addAttribute("repaymentDate", DateUtil.now("yyyy-MM-dd"));
        return "/modules/loan/loanRepayment";
    }

    @RequestMapping(value="/fireGetCustomerActiveLoansPerLoanType", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireGetCustomerActiveLoansPerLoanType(@RequestParam Map<String, String> formData){
        GeneralJsonResponse response = new GeneralJsonResponse();
        response.setStatus("200");
        response.setResult(loanCreditRepository.getCustomeActiveLoans(formData.get("loanType"),formData.get("account")));
        return response;
    }

    @RequestMapping(value="/fireInitiateLoanRepayments", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireInitiateLoanRepayments(@Valid LoanRepayment payload, BindingResult bindingResult, HttpSession session){
        GeneralJsonResponse response = new GeneralJsonResponse();
            LOGGER.info("Loan repayment init request ... {}", payload.toString());
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = bindingResult.getFieldErrors().stream()
                        .collect(
                                Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> {
                                    return error1;
                                })
                        );
                response.setStatus("FAIL");
                response.setResult(errors);
            }else{
                String reference ="MLR"+DateUtil.now("yyyyMMddhhMMss");
                String result = loanCreditRepository.initiateLoanRepayment(payload.getLoanType(),payload.getAmount(),payload.getNarration(),payload.getMsisdn(),payload.getCbsReference(),payload.getRepaymentDate(),reference,payload.getLoanId(),payload.getDebitAccount(), session.getAttribute("username").toString());
                response.setStatus(result);
                response.setResult(result);
            }
    return response;
    }

    @GetMapping("loanRepaymentApproval")
    public String loanRepaymentApproval(Model model){
        model.addAttribute("pageTitle","PENDING REPAYMENTS WAITING APPROVAL");
        return "/modules/loan/loanRepaymentApproval";
    }

    @PostMapping(value = "/fireGetLoanRepaymentTxnsAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireGetLoanRepaymentTxnsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";

        return loanCreditRepository.loanRepaymentTxnsForApproval(roleId, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireApprovalByBackOfficeLoanRepayment")
    public String fireApprovalByBackOfficeLoanRepayment(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("pageTitle", "AUTHORIZATION OF LOAN REPAYMENT WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("data", loanCreditRepository.getTransactionByReferenceNo(customeQuery.get("reference")));
        return "modules/loan/modals/previewManualLoanRepaymentForApproval";
    }

    /*
     *Authorize manual loan repayment
     */
//    @RequestMapping(value = "/fireAuthManualLoanRepaymentAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public String fireAuthManualLoanRepaymentAjax(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
//        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
//        try {
//            String postingRole = (String) httpsession.getAttribute("postingRole");
//            String reference = customeQuery.get("reference");
//            if (postingRole != null) {
//                //check if the role is allowed to process this transactions
//                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
//                for (UsRole role : user.getRoles().getRole()) {
//                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
//                        //audit trails
//                        String approver = session.getAttribute("username") + "";
//                        LOGGER.info("Approver for loan repayment req with reference.. {} IS.... {}", reference, approver);
//                        String branchNo = session.getAttribute("branchCode") + "";
//                        String roleId = session.getAttribute("roleId") + "";
//                        Map<String,Object> txnData =loanCreditRepository.getTransactionByReferenceNo(customeQuery.get("reference"));
//                        String payload ="-1";
//
//                        String url = sysenv.BRINJAL_API_URL+"/wakala/esb/loan/repayment";
////                        String url = "http://localhost:8460/esb/loan/repayment";
//                        if(sysenv.ACTIVE_PROFILE.equals("prod")){
//                            url = "http://172.21.1.13:8090/esb/loan/repayment";
//                        }
//
//                        if(txnData.get("loanType").equals("NISOGEZE")) {
//                            url = sysenv.BRINJAL_NISOGEZE_REPAYMENT_URL;
//                            payload = "{\n" +
//                                    " \"accountNo\": \""+txnData.get("nisogezeLoanId")+"\",\n" +
//                                    " \"amount\":"+txnData.get("amount")+",\n" +
//                                    " \"phoneNo\": \""+txnData.get("msisdn")+"\",\n" +
//                                    " \"repaymentDate\": \""+txnData.get("repaymentDate")+"\",\n" +
//                                    " \"description\": \""+txnData.get("descriptions")+"\",\n" +
//                                    " \"reference\": \""+txnData.get("cbsReference")+"\",\n" +
//                                    " \"repaymentType\": \""+txnData.get("repaymentType")+"\",\n" +
//                                    " \"strategy\": \""+txnData.get("repaymentStrategy")+"\",\n" +
//                                    " \"loanType\": \"NISOGEZE\"\n" +
//                                    "}";
//                        }else{
//
//                            payload = "{\n" +
//                                    " \"accountNo\": \""+txnData.get("debitAccount")+"\",\n" +
//                                    " \"amount\":"+txnData.get("amount")+",\n" +
//                                    " \"phoneNo\": \""+txnData.get("msisdn")+"\",\n" +
//                                    " \"description\": \""+txnData.get("descriptions")+"\",\n" +
//                                    " \"reference\": \""+txnData.get("reference")+"\",\n" +
//                                    " \"loanType\": \""+txnData.get("loanType")+"\"\n" +
//                                    "}";
//                        }
//                        result= HttpClientService.sendJsonRequest(payload,url);
//                        LOGGER.info("FULL RESPONSE: {}", result);
//
//                    String responseCode = XMLParserService.getDomTagText("responseCode",result);
//                    String message = XMLParserService.getRootTagText("message",result);
//                    result = "{\"result\":\""+responseCode+"\",\"message\":\""+message+"\"}";
//                        if ("0".equals(responseCode)) {
//                            // Success case: Optionally parse <loans> details
//                            List<Map<String, String>> loanItems = XMLParserService.getNestedTags(result, "loans", Arrays.asList("type", "amount", "repaymentRef"));
//                            StringBuilder details = new StringBuilder("Repayment processed:<br/>");
//                            for (Map<String, String> item : loanItems) {
//                                details.append("- ").append(item.get("type")).append(": ")
//                                        .append(item.get("amount")).append(" (")
//                                        .append(item.get("repaymentRef")).append(")").append("<br/>");
//                            }
//
//                            // Update DB status
//                            loanCreditRepository.updateTransactionStatus(reference, approver);
//                            result = "{\"result\":\"0\",\"message\":\"" + message + "\",\"details\":\"" + details + "\"}";
//                        } else {
//                            // Error case: return original root message
//                           return "{\"result\":\"" + responseCode + "\",\"message\":\"" + message + "\"}";
//                        }
//                    } else {
//                        //audit trails
//                        String username = session.getAttribute("username") + "";
//                        String branchNo = session.getAttribute("branchCode") + "";
//                        String roleId = session.getAttribute("roleId") + "";
//
//                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
//                    }
//                }
//
//            }
//
//        } catch (Exception ex) {
//            //audit trails
//            String username = session.getAttribute("username") + "";
//            String branchNo = session.getAttribute("branchCode") + "";
//            String roleId = session.getAttribute("roleId") + "";
//            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
//            LOGGER.info(null, ex);
//        }
//        return result;
//    }

    @RequestMapping(
            value = "/fireAuthManualLoanRepaymentAjax",
            method = RequestMethod.POST,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthManualLoanRepaymentAjax(
            HttpSession httpsession,
            @RequestParam Map<String, String> customeQuery,
            HttpSession session,
            HttpServletRequest request) {

        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String reference = customeQuery.get("reference");

            if (postingRole == null) {
                return result;
            }

            // check role permission
            BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
            boolean allowed = false;
            if (user != null && user.getRoles() != null && user.getRoles().getRole() != null) {
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(String.valueOf(role.getUserRoleId()))) {
                        allowed = true;
                        break;
                    }
                }
            }
            if (!allowed) {
                return "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
            }

            // audit
            String approver = String.valueOf(session.getAttribute("username"));
            LOGGER.info("Approver for loan repayment req with reference {} is {}", reference, approver);

            // prepare target URL + payload
            Map<String, Object> txnData = loanCreditRepository.getTransactionByReferenceNo(reference);
            String url;
            String payload;

            // Default
            url = sysenv.BRINJAL_API_URL + "/wakala/esb/loan/repayment";
            if ("prod".equalsIgnoreCase(sysenv.ACTIVE_PROFILE)) {
                url = "http://172.21.1.13:8090/esb/loan/repayment";
            }

            if ("NISOGEZE".equalsIgnoreCase(String.valueOf(txnData.get("loanType")))) {
                url = sysenv.BRINJAL_NISOGEZE_REPAYMENT_URL;
                payload = "{\n" +
                        "  \"accountNo\": \"" + txnData.get("nisogezeLoanId") + "\",\n" +
                        "  \"amount\": " + txnData.get("amount") + ",\n" +
                        "  \"phoneNo\": \"" + txnData.get("msisdn") + "\",\n" +
                        "  \"repaymentDate\": \"" + txnData.get("repaymentDate") + "\",\n" +
                        "  \"description\": \"" + escapeJson(String.valueOf(txnData.get("descriptions"))) + "\",\n" +
                        "  \"reference\": \"" + txnData.get("cbsReference") + "\",\n" +
                        "  \"repaymentType\": \"" + txnData.get("repaymentType") + "\",\n" +
                        "  \"strategy\": \"" + txnData.get("repaymentStrategy") + "\",\n" +
                        "  \"loanType\": \"NISOGEZE\"\n" +
                        "}";
            } else {
                payload = "{\n" +
                        "  \"accountNo\": \"" + txnData.get("debitAccount") + "\",\n" +
                        "  \"amount\": " + txnData.get("amount") + ",\n" +
                        "  \"phoneNo\": \"" + txnData.get("msisdn") + "\",\n" +
                        "  \"description\": \"" + escapeJson(String.valueOf(txnData.get("descriptions"))) + "\",\n" +
                        "  \"reference\": \"" + txnData.get("reference") + "\",\n" +
                        "  \"loanType\": \"" + txnData.get("loanType") + "\"\n" +
                        "}";
            }

            // ---- RestTemplate call ----
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            List<MediaType> mediaTypes = new ArrayList<>();
            mediaTypes.add(MediaType.APPLICATION_JSON);
            mediaTypes.add(MediaType.APPLICATION_XML);
            mediaTypes.add(MediaType.TEXT_XML);
            headers.setAccept(mediaTypes);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            String body = resp.getBody() == null ? "" : resp.getBody();

            LOGGER.info("FULL RESPONSE: {}", body);

            // ---- Parse response (supports both JSON and XML) ----
            String responseCode;
            String message;

            // Prefer Content-Type if present, fallback to sniffing
            MediaType ct = resp.getHeaders().getContentType();
            boolean looksJson = (ct != null && MediaType.APPLICATION_JSON.isCompatibleWith(ct))
                    || body.trim().startsWith("{")
                    || body.trim().startsWith("[");

            if (looksJson) {
                // JSON path (matches your "Response expected from the endpoint")
                ObjectMapper om = new ObjectMapper();
                JsonNode root = om.readTree(body);

                responseCode = textOf(root.get("responseCode"));
                message = textOf(root.get("message"));

                if ("0".equals(responseCode)) {
                    // build details from loans[]
                    StringBuilder details = new StringBuilder("Repayment processed:<br/>");
                    if (root.has("loans") && root.get("loans").isArray()) {
                        for (var n : root.get("loans")) {
                            String type = textOf(n.get("type"));
                            String amount = textOf(n.get("amount"));
                            String repaymentRef = textOf(n.get("repaymentRef"));
                            details.append("- ").append(type).append(": ").append(amount)
                                    .append(" (").append(repaymentRef).append(")").append("<br/>");
                        }
                    }

                    // success → update DB and return
                    loanCreditRepository.updateTransactionStatus(reference, approver);
                    result = "{\"result\":\"0\",\"message\":\"" + escapeJson(message) + "\",\"details\":\"" + escapeJson(details.toString()) + "\"}";
                } else {
                    result = "{\"result\":\"" + responseCode + "\",\"message\":\"" + escapeJson(message) + "\"}";
                }

            } else {
                // XML path (your logs show <LoanRepaymentResp>…)
                String cleaned = sanitizeXml(body);
                // you can keep using your XMLParserService if you prefer; here we rely on its helpers:
                String topCode = XMLParserService.getDomTagText("responseCode", cleaned);
                String topMsg  = XMLParserService.getRootTagText("message", cleaned);
                responseCode = topCode;
                message = topMsg;

                if ("0".equals(responseCode)) {
                    // Extract loan-level details from <loans><loans>…</loans></loans>
                    // (If your XMLParserService.getNestedTags expects (xml, parentTag, wantedFields))
                    java.util.List<String> fields = java.util.Arrays.asList("type", "amount", "repaymentRef");
                    java.util.List<java.util.Map<String, String>> loanItems =
                            XMLParserService.getNestedTags(cleaned, "loans", fields);

                    StringBuilder details = new StringBuilder("Repayment processed:<br/>");
                    for (java.util.Map<String, String> item : loanItems) {
                        details.append("- ").append(item.getOrDefault("type", ""))
                                .append(": ").append(item.getOrDefault("amount", ""))
                                .append(" (").append(item.getOrDefault("repaymentRef", ""))
                                .append(")").append("<br/>");
                    }

                    loanCreditRepository.updateTransactionStatus(reference, approver);
                    result = "{\"result\":\"0\",\"message\":\"" + escapeJson(message) + "\",\"details\":\"" + escapeJson(details.toString()) + "\"}";
                } else {
                    result = "{\"result\":\"" + responseCode + "\",\"message\":\"" + escapeJson(message) + "\"}";
                }
            }

            return result;

        } catch (Exception ex) {
            String username = String.valueOf(session.getAttribute("username"));
            String branchNo = String.valueOf(session.getAttribute("branchCode"));
            String roleId   = String.valueOf(session.getAttribute("role.Id"));
            LOGGER.error("General error in fireAuthManualLoanRepaymentAjax, user={}, branch={}, role={}", username, branchNo, roleId, ex);
            return "{\"result\":\"99\",\"message\":\"General Error occured: " + escapeJson(ex.getMessage()) + " \"}";
        }
    }


    @GetMapping("/fireDownloadCsvPerLoanType")
    public void downloadSampleCsvForLoanRepayment(@RequestParam Map<String,String> customeQuery, HttpServletResponse response) throws IOException {
        try {
            String loanType = customeQuery.get("loanType");
            String fileName = "attachment; filename="+loanType + "SAMPLE" + DateUtil.now("yyyyMMddhhMMss") + ".csv";
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", fileName);
            PrintWriter writer = response.getWriter();
            String[] csvHeader = {"LOAN_TYPE","REPAYMENT_DATE", "AMOUNT", "CBS_REF", "NISOGEZE_LOAN_ID", "PHONE_NO", "DESCRIPTIONS"};

            if(loanType.equalsIgnoreCase("NISOGEZE")) {
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(csvHeader));
                csvPrinter.printRecord(loanType,DateUtil.now("yyyy-MM-dd"), "10000", "KDS20240628100626", "999NS2102921", "255716219069", "Nisogeze Loan repayment for 999NS2102921 amount 10000 reference: KDS20240628100626");
                csvPrinter.flush();
                csvPrinter.close();
            }else{
                csvHeader = new String[]{"LOAN_TYPE","DEBIT_ACCT", "AMOUNT", "DESCRIPTIONS"};
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(csvHeader));
                csvPrinter.printRecord(loanType,"11021000282", "10000", "Loan repayment for "+loanType+" amount 10000 reference: KDS20240628100626");
                csvPrinter.flush();
                csvPrinter.close();
            }

        }catch(Exception ex) {
            LOGGER.info(null,ex);
        }
    }

    @RequestMapping(
            value = "fireUploadBatchForRepayment",
            method = RequestMethod.POST,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String uploadBatch(@RequestParam("batchFile") MultipartFile batchFile,
                              @RequestParam Map<String,String> customeQuery,
                              HttpSession session) {
        if (batchFile == null || batchFile.isEmpty()) {
            return "{\"code\":\"99\",\"message\":\"Invalid batch\"}";
        }

        final String loanTypeSelected = nvl(customeQuery.get("loanType"));
        final String username         = String.valueOf(session.getAttribute("username"));
        final String repaymentType    = nvl(customeQuery.get("repaymentType"));
        final String repaymentStrategy= nvl(customeQuery.get("repaymentStrategy"));

        final String batchRef = loanTypeSelected + username + DateUtil.now("yyyyMMddhhMMss");

        try (Reader reader = new InputStreamReader(batchFile.getInputStream())) {

            CSVFormat fmt = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim()
                    .withIgnoreEmptyLines(true)
                    .withIgnoreSurroundingSpaces(true);

            try (org.apache.commons.csv.CSVParser parser = fmt.parse(reader)) {

                // Validate headers exist (case-insensitive)
                Map<String, Integer> headerMap = parser.getHeaderMap();
                requireHeader(headerMap, "LOAN_TYPE");
                requireHeader(headerMap, "REPAYMENT_DATE");
                requireHeader(headerMap, "AMOUNT");
                requireHeader(headerMap, "NISOGEZE_LOAN_ID");
                requireHeader(headerMap, "PHONE_NO");

                List<CSVRecord> rows = parser.getRecords();
                if (rows.isEmpty()) {
                    return "{\"code\":\"99\",\"message\":\"Empty CSV\"}";
                }

                // Ensure the batch is for the selected loan type
                String firstLoanType = nvl(rows.get(0).get("LOAN_TYPE"));
                if (!firstLoanType.equalsIgnoreCase(loanTypeSelected)) {
                    return "{\"code\":\"99\",\"message\":\"Invalid Loan type selected\"}";
                }

                // Ready to log the batch
                loanCreditRepository.logLoanRepaymentSummary(batchRef, "LOGGED");

                int count = 1;
                for (CSVRecord r : rows) {
                    final String rowLoanType  = nvl(r.get("LOAN_TYPE"));

                    if ("NISOGEZE".equalsIgnoreCase(rowLoanType)) {
                        String repaymentDate = nvl(r.get("REPAYMENT_DATE"));
                        BigDecimal amount    = toAmount(r.get("AMOUNT"));

                        String loanId        = nvl(r.get("NISOGEZE_LOAN_ID"));
                        String phoneNo       = nvl(r.get("PHONE_NO"));
                        // Derive CBS reference per loanId, then make unique with counter
                        String cbsRef  = cbsReference(loanId);
                        String reference = cbsRef + "_" + count;
                        String descriptions  = buildNisogezeDescription(loanId, amount, cbsRef);

                        loanCreditRepository.logNisogezeLoanRepayment(
                                rowLoanType,
                                "173208000074",           // debit account? (confirm if static)
                                amount,
                                cbsRef,
                                reference,
                                loanId,
                                batchRef,
                                phoneNo,
                                descriptions,
                                DateUtil.now(),
                                repaymentDate,
                                username,
                                repaymentType,
                                repaymentStrategy
                        );
                    } else {
                        // Generic loan type path: LOAN_TYPE,DEBIT_ACCT,AMOUNT,DESCRIPTIONS
                        String debitAcct   = nvl(r.get(1)); // or use header if you have one
                        BigDecimal amount  = toAmount(r.get(2));
                        String descriptions= nvl(r.get(3));
                        String reference   = "MLR" + DateUtil.now("yyyyMMddhhMMss") + "_" + count;

                        loanCreditRepository.logLoanRepayment(
                                rowLoanType, debitAcct, amount, descriptions, reference, batchRef, username
                        );
                    }
                    count++;
                }

                return "{\"code\":\"200\",\"message\":\"Batch initiated successfully\"}";
            }

        } catch (Exception e) {
            LOGGER.error("Batch upload failed", e);
            return "{\"code\":\"99\",\"message\":\"General failure: " + safe(e.getMessage()) + "\"}";
        }
    }

    /* ---------- helpers ---------- */

    private static String nvl(String s) { return s == null ? "" : s.trim(); }

    private static void requireHeader(Map<String,Integer> headerMap, String key) {
        boolean ok = headerMap.keySet().stream().anyMatch(h -> h.equalsIgnoreCase(key));
        if (!ok) throw new IllegalArgumentException("Missing column: " + key);
    }

    private static String buildNisogezeDescription(String loanId, BigDecimal amount, String cbsRef) {
        return String.format(
                "Nisogeze Loan repayment for %s amount %s reference: %s",
                nvl(loanId),
                formatAmount(amount),
                nvl(cbsRef)
        );
    }

    private static String formatAmount(BigDecimal amt) {
        if (amt == null) return "0";
        // Match your sample: plain number, no commas, no trailing .00
        return amt.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal toAmount(String s) {
        if (s == null) return BigDecimal.ZERO;
        String clean = s.replace(",", "").trim();
        if (clean.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(clean);
    }

    private static String safe(String msg) {
        return msg == null ? "" : msg.replace("\"", "'");
    }

    @PostMapping(value = "/fireUpdatedCustomerLoansAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> fetchCustomerLoans(@RequestParam Map<String,String> query) {
        String fromDate = query.get("fromDate");
        String toDate = query.get("toDate");
        NisogezeReportRequestDTO request = new NisogezeReportRequestDTO(query.get("loanType"),fromDate, toDate);
        String requestPayload = Mapper.toJson(request);
        String apiUrl = sysenv.BRINJAL_API_BASE_URL+"/esb/api/v1/reports/nisogeze/outstanding";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestPayload,headers);
        ParameterizedTypeReference<List<PortfolioPositionReportDTO>> typeReference =
                new ParameterizedTypeReference<List<PortfolioPositionReportDTO>>() {};
        try {
            ResponseEntity<List<PortfolioPositionReportDTO>> response= restTemplate.exchange(apiUrl, HttpMethod.POST,entity, typeReference);
            log.info("Response: {}", response);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        }catch (Exception e){
            log.error("Error response: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error response: "+e.getMessage());
        }
    }

    @PostMapping(value = "/fireGetChargedOffLoanFromBrinjal", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<List<ChargedOffDTO>> fetchChargedOffLoan(@RequestParam Map<String,String> query) {
        String fromDate = query.get("fromDate");
        String toDate = query.get("toDate");
        NisogezeReportRequestDTO request = new NisogezeReportRequestDTO(query.get("loanType"),fromDate,toDate);
        String requestPayload = Mapper.toJson(request);
        String apiUrl = sysenv.BRINJAL_API_BASE_URL+"/api/v1/reports/nisogeze/charged-off";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestPayload,headers);
        ParameterizedTypeReference<List<ChargedOffDTO>> reference = new ParameterizedTypeReference<List<ChargedOffDTO>>() {};
        try {
            ResponseEntity<List<ChargedOffDTO>> response= restTemplate.exchange(apiUrl, HttpMethod.POST,entity, reference);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Error response: {}", e.getMessage());
        }


        return ResponseEntity.noContent().build();
    }

    private static String textOf(com.fasterxml.jackson.databind.JsonNode n) {
        return (n == null || n.isNull()) ? "" : n.asText();
    }
    /** Remove anything before first '<', BOM, and illegal control chars that break XML parsers. */
    private static String sanitizeXml(String raw) {
        if (raw == null) return "";
        String s = raw;
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
        int i = s.indexOf('<');
        if (i > 0) s = s.substring(i);
        s = s.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\uFFFF]", "");
        return s.trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }


}
