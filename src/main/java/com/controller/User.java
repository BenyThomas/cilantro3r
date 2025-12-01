/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.Reports.AuditTrails;
import com.core.MY_Controller;
import com.entities.ReconTypeMappingForm;
import com.entities.ReportsJsonResponse2;
import com.entities.RoleForm;
import com.entities.UserForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.repository.SftpRepo;
import com.repository.User_M;
import com.service.TransferService;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import philae.api.BnUser;
import philae.api.UsRole;

/**
 *
 * @author MELLEJI
 */
@Controller
public class User {

    @Autowired
    HttpSession httpSession;

    @Autowired
    User_M user_m;
    @Autowired
    ObjectMapper jacksonMapper;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(User.class);

    @RequestMapping(value = {"/login", "/"})
    //@ResponseBody
    public String login(Model model, HttpServletResponse response) throws ParseException {
     //   response.setHeader("Content-Security-Policy", "style-src 'self'; script-src 'self'; form-action 'self'");
        return "login";
    }

    @RequestMapping(value = "/addUser")
    public String addUser(HttpSession session, Model model) {
//        AuditTrails.setComments("Add user to the system");
//        AuditTrails.setFunctionName("/addUser");
        UserForm userForm = new UserForm();
        model.addAttribute("roles", user_m.getRoles());
        model.addAttribute("users", user_m.getAllUsers());
        model.addAttribute("userDetails", userForm);
        model.addAttribute("custid", "0");
        model.addAttribute("branches", user_m.branches());
        return "user/addUser";
    }

    @RequestMapping(value = "/editUser")
    public String editUser(HttpSession session, Model model, @RequestParam Map<String, String> customeQuery) {
        UserForm userForm = new UserForm();
        String custId = "";
        if (customeQuery.get("user") != null) {
            LOGGER.info("USER ID:{}", customeQuery.get("user"));
            String userId = customeQuery.get("user").split("PQRSTDR")[1];
            userForm = user_m.getUserDetails(userId);
            custId = userForm.getTrackingId();
        }
        model.addAttribute("roles", user_m.getRoles());
        model.addAttribute("users", user_m.getAllUsers());
        model.addAttribute("userDetails", userForm);
        model.addAttribute("custid", custId);
        model.addAttribute("branches", user_m.branches());
        return "user/addUser";
    }

    @RequestMapping(value = "/permissionDenied")
    public String permissionDenied(HttpSession session) {
//        AuditTrails.setComments("Permission Denied to view this Page");
//        AuditTrails.setFunctionName("/permissionDenied");
        return "pages/permissionDenied";
    }

    @RequestMapping(value = {"/saveUser"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 saveUser(@Valid UserForm userForm, BindingResult result, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            LOGGER.info("VALIDATION PASSED USER CREATION REQUEST:{}", userForm.toString());
            respone.setValidated(true);
            //save the records on the database
            String custid = customeQuery.get("custid");
            int res = -1;
            if (custid.equals("0")) {
                res = user_m.saveUser(userForm.getFirstName(), userForm.getMiddleName(), userForm.getLastName(), userForm.getUsername(), userForm.getEmail(), userForm.getPhone(), userForm.getRole(), session.getAttribute("username").toString(), userForm.getBranchCode(), userForm.getUserStatus());
            } else {
                res = user_m.editUser(userForm, session.getAttribute("username").toString(), custid);
            }
            LOGGER.info("RESPONSE FROM SAVING/EDITING USER:{}", res);
            if (res == 1) {
                respone.setValidated(true);
            } else {
                respone.setValidated(false);

            }

        }
        AuditTrails.setComments("Save the newly created user to the database: user created:" + userForm.getUsername());
        AuditTrails.setFunctionName("/saveUser");
        return respone;
    }

    @RequestMapping(value = "/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("session", httpSession.getAttribute("modules"));
//        AuditTrails.setComments("You have loggedin successfully welcome to the dashboard");
//        AuditTrails.setFunctionName("/dashboard");
        return "pages/dashboard";
    }

    @RequestMapping(value = {"/downloadUsersReportMatrix"}, method = {RequestMethod.GET})
    @ResponseBody
    public String downloadUsersReportMatrix(@RequestParam Map<String, String> customeQuery, HttpServletResponse res, HttpSession session) throws IOException {
        return user_m.getUserMatrixReport(customeQuery.get("reportFormat"), res, "cilantroUserMatrix" + DateUtil.now("yyyy-MM-dd-HHmmss"), session.getAttribute("username").toString());

    }

    @RequestMapping(value = "/dashboard2")
    public String dashboard2(Model model) {
        model.addAttribute("session", httpSession.getAttribute("modules"));
//        AuditTrails.setComments("You have logged in successfully welcome to the dashboard");
//        AuditTrails.setFunctionName("/dashboard2");
        return "pages/dashboard";
    }

    @RequestMapping(value = "/manageUsers")
    public String manageUsers(HttpSession session) {
//        AuditTrails.setComments("View all Users of the system");
//        AuditTrails.setFunctionName("/manageUsers");
        return "user/manageUsers";
    }

    @RequestMapping(value = "/addRole")
    public String addRole(HttpSession session, Model model) {
        model.addAttribute("permissions", user_m.getModulesPermissions());
        model.addAttribute("reconTypes", user_m.getReconTypes());
        model.addAttribute("generalReconTypes", user_m.getGeneralReconTypes());
        model.addAttribute("operations", user_m.getReconOperationsTypes());
        model.addAttribute("transferTypes", user_m.getTransferTypes());
        model.addAttribute("roles", user_m.getRoles());
        model.addAttribute("modules", user_m.getModules());
        model.addAttribute("paymentModules", user_m.getPaymentsModules());
        model.addAttribute("paymentPermissions", user_m.getPaymentsPermissions());
        model.addAttribute("kycModules", user_m.getKycModules());
//        AuditTrails.setComments("View create a new Role on the system");
//        AuditTrails.setFunctionName("/addRole");
        return "user/addRole";
    }

    @RequestMapping(value = "/editRole")
    public String editRole(HttpSession session, Model model, @RequestParam Map<String, String> customeQuery) {
        try {
            /**
             * check the permission allowed per module
             */
            String permsTag = "";
            String paymentsModulesTag = "";
            String reconType = "";
            String generalReconType = "";
            String transferType = "";
            String OperationType = "";
            String kycTag = "";
            List<Map<String, Object>> rolePermission = user_m.getPermissionPerModule(customeQuery.get("role_id"));
            List<Map<String, Object>> allPermission = user_m.getModulesPermissions();
            List<Map<String, Object>> modules = user_m.getModules();
            List<Map<String, Object>> reconTypePerRole = user_m.getReconTypePerRole(customeQuery.get("role_id"));
            List<Map<String, Object>> operationPerRole = user_m.getReconOperationPerRole(customeQuery.get("role_id"));
            List<Map<String, Object>> transferTypePerRole = user_m.getTransferTypesPerRole(customeQuery.get("role_id"));
            List<Map<String, Object>> paymentsModulePerRole = user_m.getPaymentsModulePerRole(customeQuery.get("role_id"));
            List<Map<String, Object>> kycModulePerRole = user_m.getKycModulesPerRole(customeQuery.get("role_id"));
            List<Map<String, Object>> generalReconTypePerRole = user_m.getGeneralReconTypes(customeQuery.get("role_id"));

            String checked = "";
            System.out.println("TRANSFER TYPES:" + transferTypePerRole);
            String trStyle = null;
            for (Map<String, Object> module : modules) {
                permsTag += "<tr>\n";
                permsTag += "<td>"
                        + module.get("name")
                        + "</td>\n<td>";
                for (Map<String, Object> item : allPermission) {
                    for (Map<String, Object> inItem : rolePermission) {
                        if (item.get("permID").equals(inItem.get("permission_id"))) {
                            checked = "checked";
                        }
                    }

                    if (module.get("id").equals(item.get("moduleID"))) {
                        permsTag += "<input class='permissions' " + checked + " style='margin-left: 1% ! important;'  type='checkbox' name='permission'  value='" + item.get("permID") + "__" + module.get("id") + "'/>"
                                + item.get("permissionName")
                                + ""
                                + "";
                    }
                    checked = "";
                }
                permsTag += "</td></tr>\n";

            }
            for (Map<String, Object> operations : user_m.getReconOperationsTypes()) {
                for (Map<String, Object> operPerRole : operationPerRole) {
                    if (operPerRole.get("recon_operation_id").equals(operations.get("id"))) {
                        checked = "checked";
                    }
                }
                OperationType += "<input class='operation' " + checked + " style='margin-left: 1% ! important;'  type='checkbox' name='operation'  value='" + operations.get("id") + "'/>" + operations.get("name");
                checked = "";

            }
            for (Map<String, Object> reconTypes : user_m.getReconTypes()) {
                for (Map<String, Object> reconPerRole : reconTypePerRole) {
                    if (reconPerRole.get("recon_type_id").equals(reconTypes.get("id"))) {
                        checked = "checked";
                    }
                }
                reconType += "<input class='reconType' " + checked + " style='margin-left: 1% ! important;'  type='checkbox' name='reconType'  value='" + reconTypes.get("id") + "'/>" + reconTypes.get("name");
                ;
                checked = "";

            }
            for (Map<String, Object> reconTypes : user_m.getGeneralReconTypes()) {
                for (Map<String, Object> reconPerRole : generalReconTypePerRole) {
                    if (reconPerRole.get("grc_id").equals(reconTypes.get("id"))) {
                        checked = "checked";
                    }
                }
                generalReconType += "<input class='generalReconType' " + checked + " style='margin-left: 1% ! important;'  type='checkbox' name='generalReconType'  value='" + reconTypes.get("id") + "'/>" + reconTypes.get("displayName");
                checked = "";

            }
            //transfer types
            for (Map<String, Object> transferTypes : user_m.getTransferTypes()) {
                for (Map<String, Object> transferPerRole : transferTypePerRole) {
                    if (transferPerRole.get("transfer_type_id").equals(transferTypes.get("id"))) {
                        checked = "checked";
                    }
                }
                transferType += "<input class='transferType' " + checked + " style='margin-left: 1% ! important;'  type='checkbox' name='transferType'  value='" + transferTypes.get("id") + "'/>" + transferTypes.get("name");
                ;
                checked = "";

            }
            //Posting permission
            for (Map<String, Object> module : user_m.getPaymentsModules()) {
                paymentsModulesTag += "<tr>\n";
                paymentsModulesTag += "<td>"
                        + module.get("name")
                        + "</td>\n<td>";
                for (Map<String, Object> item : user_m.getPaymentsPermissions()) {
                    for (Map<String, Object> inItem : user_m.getPaymentPermissionPerModule(customeQuery.get("role_id"))) {
                        if (item.get("permID").equals(inItem.get("payment_permission_id"))) {
                            checked = "checked";
                        }
                    }

                    if (module.get("id").equals(item.get("moduleID"))) {
                        paymentsModulesTag += "<input class='ppermissions' " + checked + " style='margin-left: 1% ! important;'  type='checkbox' name='Paymentpermission'  value='" + item.get("permID") + "__" + module.get("id") + "'/>"
                                + item.get("permissionName")
                                + ""
                                + "";
                    }
                    checked = "";
                }
                paymentsModulesTag += "</td></tr>\n";

            }
            //kyc
            for (Map<String, Object> kycModules : user_m.getKycModules()) {
                for (Map<String, Object> kycPerRole : kycModulePerRole) {
                    if (kycModules.get("id").equals(kycPerRole.get("kyc_permission_id"))) {
                        checked = "checked";
                    }
                }
                kycTag += "<input class='kyc' " + checked + " style='margin-left: 1% ! important;' type='checkbox' name='kyc' value='" + kycModules.get("id") + "'/>" + kycModules.get("name");
                checked = "";
            }
            model.addAttribute("roleTags", permsTag);
            model.addAttribute("paymentsModulesTag", paymentsModulesTag);
            model.addAttribute("reconTypesTag", "<tr><td>Recon types</td><td>" + reconType + "</td></tr>");
            model.addAttribute("generalReconTypesTag", "<tr><td>Recon types</td><td>" + generalReconType + "</td></tr>");
            model.addAttribute("transferTypeTag", "<tr><td>Transfer Types</td><td>" + transferType + "</td></tr>");
            model.addAttribute("operationsTag", "<tr><td>Operations</td><td>" + OperationType + "</td></tr>");
            model.addAttribute("roles", user_m.getRole(customeQuery.get("role_id")));
            model.addAttribute("modules", user_m.getModules());
            model.addAttribute("kycModulesTag", "<tr><td>KYC</td><td>" + kycTag + "</td></tr>");
            AuditTrails.setComments("Editing the Role: " + customeQuery.get("role_id"));
            AuditTrails.setFunctionName("/editRole");
        }catch (Exception e){
            e.printStackTrace();
            LOGGER.error(e.getMessage(),e);
            return null;
        }
        return "user/editRole";
    }

    @RequestMapping(value = {"/saveRole"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 saveRole(@Valid RoleForm roleForm, BindingResult result, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            //save the records on the database
            System.out.println("Paymentpermission: " + customeQuery.get("Paymentpermission"));
            if (customeQuery.get("role_id").equals("-1")) {
                user_m.saveRole(roleForm.getRoleName(), roleForm.getDescription(), customeQuery.get("permissionID"), customeQuery.get("reconTypeID"), customeQuery.get("generalReconTypeID"), customeQuery.get("operationID"), customeQuery.get("moduleId"), session.getAttribute("username").toString(), customeQuery.get("transferTypeID"), customeQuery.get("Paymentpermission"), customeQuery.get("kycID"));
            } else {
                System.out.println("editing role: ");
                user_m.editRole(roleForm.getRoleName(), roleForm.getDescription(), customeQuery.get("permissionID"), customeQuery.get("reconTypeID"), customeQuery.get("generalReconTypeID"), customeQuery.get("operationID"), customeQuery.get("moduleId"), customeQuery.get("role_id"), session.getAttribute("username").toString(), customeQuery.get("transferTypeID"), customeQuery.get("Paymentpermission"), customeQuery.get("kycID"));

            }

        }
//        AuditTrails.setComments("Saving the role on the database: Role Name created=" + roleForm.getRoleName());
//        AuditTrails.setFunctionName("/saveRole");
        return respone;
    }

    @RequestMapping(value = "/fireUsersList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCmsPartnersAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        AuditTrails.setComments("Viewing the users List on the system");
        AuditTrails.setFunctionName("/fireUsersList");
        return user_m.getUsersList(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getUsersAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getUsersAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return user_m.getUserListsAjax(customeQuery.get("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = {"/logout"}, method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        HttpSession session = request.getSession(false);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        for (Cookie cookie : request.getCookies()) {
            cookie.setMaxAge(0);
        }

        AuditTrails.setComments("You have successfully Logged out");
        AuditTrails.setFunctionName("/logout");
        return "redirect:/login?logout";
    }

    @RequestMapping(value = "/selectOpRole")
    public String selectOpRole(Model model) {
        BnUser user = (BnUser) httpSession.getAttribute("userCorebanking");
        model.addAttribute("user", user);
        AuditTrails.setComments("Select Operational Role");
        AuditTrails.setFunctionName("/selectOpRole");
        return "user/selectOpRole";
    }

    @RequestMapping(value = "/setPostingRoleInSession", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String setPostingRoleInSession(Model model, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
//        System.out.println("POSTING ROLE IN SESSION: " + customeQuery.get("selectedRole"));
        Long selectedRole = Long.parseLong(customeQuery.get("selectedRole"));
        httpSession.setAttribute("postingRole", customeQuery.get("selectedRole"));
        BnUser user = (BnUser) httpSession.getAttribute("userCorebanking");
        for (UsRole role : user.getRoles().getRole()) {
            if (selectedRole.toString().equalsIgnoreCase(role.getUserRoleId().toString())) {
                httpSession.setAttribute("operationRole", role.getRole().toUpperCase(Locale.ITALY));
                break;
            }
        }
        AuditTrails.setComments("set posting role in session");
        AuditTrails.setFunctionName("/setPostingRoleInSession");
        return "{\"Role selected successfully\"}";
    }

    /*
    GET TACH/EFT/RTGS POSTING USER ROLE
     */
    public philae.ach.UsRole getAchRole(philae.ach.BnUser user, String roleInSession) {
        philae.ach.UsRole rol = null;
        for (philae.ach.UsRole role : user.getRoles().getRole()) {
            if (roleInSession.equalsIgnoreCase(role.getUserRoleId().toString())) {
                rol = role;
            }
        }
        return rol;

    }
//  @RequestMapping(value = "/error")
//    public String errorView() {
//        return "error";
//    }
}
