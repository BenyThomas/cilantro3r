package com.controller.administration;

import com.DTO.administration.*;
import com.entities.ReportsJsonResponse2;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.repository.administration.AdministrationRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdministrationController {

    @Autowired
    AdministrationRepository administrationRepository;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AdministrationController.class);

    @RequestMapping(value = "/fireAdministrationDashboard", method = RequestMethod.GET)
    public String administrationDashboard(Model model, HttpSession session, @RequestParam Map<String, String> params) {
        model.addAttribute("pageTitle", "ADMINISTRATION DASHBOARD");
        model.addAttribute("userId", session.getAttribute("userId").toString());
        model.addAttribute("modules", administrationRepository.getAdministrationModules(session.getAttribute("roleId").toString()));
        return "modules/administration/dashboard";
    }

    @RequestMapping(value = "/fireRiskIndicators", method = RequestMethod.GET)
    public String riskIndicators(Model model, HttpSession session, @RequestParam Map<String, String> params) {
        model.addAttribute("pageTitle", "KEY RISK INDICATORS");
        model.addAttribute("userId", session.getAttribute("userId").toString());
        model.addAttribute("roleId", session.getAttribute("roleId"));
        model.addAttribute("branchCode", session.getAttribute("branchCode"));
        model.addAttribute("branches", administrationRepository.branches());
        model.addAttribute("indicators", administrationRepository.indicators());
        return "modules/administration/riskIndicators";
    }

    @RequestMapping(value = "/fireCostOptimizations", method = RequestMethod.GET)
    public String costOptimizations(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "COST OPTIMIZATIONS");
        model.addAttribute("userId", session.getAttribute("userId").toString());
        model.addAttribute("roleId", session.getAttribute("roleId"));
        model.addAttribute("branchCode", session.getAttribute("branchCode"));
        model.addAttribute("branches", administrationRepository.branches());
        model.addAttribute("parameters", administrationRepository.parameters());
        return "modules/administration/costOptimizations";
    }

    @RequestMapping(value = "/fireCostOptimizationReport", method = RequestMethod.GET)
    public String costOptimizationReport(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "REPORTS");
        model.addAttribute("userId", session.getAttribute("userId").toString());
        model.addAttribute("roleId", session.getAttribute("roleId"));
        List<String> branches = administrationRepository.branchNames();
        model.addAttribute("branches", branches);

        List<String> columns = administrationRepository.parameterColumns();
        List<Map<String, Object>> rows = administrationRepository.parameterValues();

        // Build lookup: branch -> (column -> value)
        Map<String, Map<String, String>> table = new HashMap<>();
        for (String branch: branches) {
            Map<String, String> map = new HashMap<>();
            for (String column: columns) {
                map.put(column, "-");
            }
            table.putIfAbsent(branch, map);
        }
        for (Map<String, Object> row : rows) {
            String branch = row.get("branch").toString();
            String col = row.get("service").toString();
            String val = row.get("cost").toString();

            table.get(branch).put(col, val);
        }
        model.addAttribute("columns", columns);
        model.addAttribute("table", table);

        return "modules/administration/costOptimizationReport";
    }

    @RequestMapping(value = "/fireRiskIndicatorReport", method = RequestMethod.GET)
    public String riskIndicatorReport(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "REPORTS");
        model.addAttribute("userId", session.getAttribute("userId").toString());
        model.addAttribute("roleId", session.getAttribute("roleId"));
        List<String> branches = administrationRepository.branchNames();
        model.addAttribute("branches", branches);

        List<String> columns = administrationRepository.indicatorColumns();
        List<Map<String, Object>> rows = administrationRepository.indicatorValues();

        // Build lookup: branch -> (column -> value)
        Map<String, Map<String, String>> table = new HashMap<>();
        for (String branch: branches) {
            Map<String, String> map = new HashMap<>();
            for (String column: columns) {
                map.put(column, "-");
            }
            table.putIfAbsent(branch, map);
        }
        for (Map<String, Object> row : rows) {
            String branch = row.get("branch").toString();
            String col = row.get("indicator").toString();
            String val = row.get("value").toString();

            table.get(branch).put(col, val);
        }
        model.addAttribute("columns", columns);
        model.addAttribute("table", table);

        return "modules/administration/riskIndicatorsReport";
    }

    @RequestMapping(value = "/fireGetCostOptimizationsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCostOptimizations(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String fromDate = params.get("fromDate") + " 00:00:00";
        String toDate = params.get("toDate") + " 23:59:59";
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";
        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        String branch = params.get("branch");
        return administrationRepository.getCostOptimizationsInfo(
                session.getAttribute("branchCode").toString(), branch, fromDate, toDate, draw, start,
                rowPerPage, searchValue, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireGetRiskIndicatorsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRiskIndicators(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String fromDate = params.get("fromDate") + " 00:00:00";
        String toDate = params.get("toDate") + " 23:59:59";
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";
        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        String branch = params.get("branch");
        String category = params.get("category");
        String type = params.get("type");
        return administrationRepository.getRiskIndicatorsInfo(
                session.getAttribute("branchCode").toString(), branch, category, type, fromDate, toDate, draw, start,
                rowPerPage, searchValue, columnName, columnSortOrder);
    }

    @RequestMapping(value = {"/fireAddCostOptimization"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 addCostOptimization(@Valid AddCostOptimizationParameter costOptimizationForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            costOptimizationForm.setCreatedBy(session.getAttribute("username").toString());
            if (costOptimizationForm.getCreatedDate() == null || costOptimizationForm.getCreatedDate().isEmpty()) {
                costOptimizationForm.setCreatedDate(DateUtil.now());
            }
            String json = administrationRepository.addCostOptimization(costOptimizationForm);
            response.setValidated(true);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                response.setJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("FORM DETAILS:{}", costOptimizationForm);

        return response;
    }

    @RequestMapping(value = {"/fireAddRiskIndicator"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 addRiskIndicator(@Valid AddRiskIndicatorService riskIndicatorForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            riskIndicatorForm.setCreatedBy(session.getAttribute("username").toString());
            if (riskIndicatorForm.getCreatedDate() == null || riskIndicatorForm.getCreatedDate().isEmpty()) {
                riskIndicatorForm.setCreatedDate(DateUtil.now());
            }
            String json = administrationRepository.addRiskIndicator(riskIndicatorForm);
            response.setValidated(true);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                response.setJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("FORM DETAILS:{}", riskIndicatorForm);

        return response;
    }

    @RequestMapping(value = {"/fireSubmitCostOptimizationForm"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 branchAddCostOptimization(@Valid AddCostOptimizationForm costOptimizationForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            costOptimizationForm.setCreatedBy(session.getAttribute("username").toString());
            if (costOptimizationForm.getCreatedDate() == null) {
                costOptimizationForm.setCreatedDate(new Date());
            }
            String json = administrationRepository.branchAddCostOptimization(costOptimizationForm);
            response.setValidated(true);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                response.setJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("FORM DETAILS:{}", costOptimizationForm);

        return response;
    }

    @RequestMapping(value = {"/fireEditCostOptimizationForm"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 branchEditCostOptimization(@Valid EditCostOptimizationForm editCostOptimizationForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            editCostOptimizationForm.setUpdatedBy(session.getAttribute("username").toString());
            editCostOptimizationForm.setUpdatedDate(new Date());
            String json = administrationRepository.branchEditCostOptimization(editCostOptimizationForm);
            response.setValidated(true);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                response.setJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("EDIT FORM DETAILS:{}", editCostOptimizationForm);

        return response;
    }

    @RequestMapping(value = {"/fireRemoveCostOptimization"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String branchRemoveCostOptimization(@RequestParam Map<String, String> params, HttpSession session) {
        return administrationRepository.branchRemoveCostOptimization(params.get("id"));
    }

    @RequestMapping(value = {"/fireSubmitRiskIndicatorForm"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 branchAddRiskIndicator(@Valid NewRiskIndicatorForm riskIndicatorForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            riskIndicatorForm.setCreatedBy(session.getAttribute("username").toString());
            if (riskIndicatorForm.getCreatedDate() == null) {
                riskIndicatorForm.setCreatedDate(new Date());
            }
            String json = administrationRepository.branchAddRiskIndicator(riskIndicatorForm);
            response.setValidated(true);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                response.setJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("FORM DETAILS:{}", riskIndicatorForm);

        return response;
    }

    @RequestMapping(value = {"/fireEditRiskIndicatorForm"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 branchEditRiskIndicator(@Valid EditRiskIndicatorForm editRiskIndicatorForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            editRiskIndicatorForm.setUpdatedBy(session.getAttribute("username").toString());
            editRiskIndicatorForm.setUpdatedDate(new Date());
            String json = administrationRepository.branchEditRiskIndicator(editRiskIndicatorForm);
            response.setValidated(true);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(json, Map.class);
                response.setJson(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("EDIT FORM DETAILS:{}", editRiskIndicatorForm);

        return response;
    }

    @RequestMapping(value = {"/fireRemoveRiskIndicator"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String branchRemoveRiskIndicator(@RequestParam Map<String, String> params, HttpSession session) {
        return administrationRepository.branchRemoveRiskIndicator(params.get("id"));
    }
}
