package com.controller.itax.CMSpartners;

import com.DTO.cms.CMSPartnerForm;
import com.DTO.tips.TipsJsonResponse;
import com.controller.itax.ItaxJsonResponse;
import com.repository.itax.CMSPartners.CMSPartnerRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CMSPartnersController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CMSPartnersController.class);

    @Autowired
    CMSPartnerRepository cmsPartnerRepository;

    @RequestMapping("/firePortalAdminModule")
    public String cmsPartners(){
        return "/itax/portal_admin/cmsPartners";
    }

    @RequestMapping(value = "/fireCMSPartnersAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCmsPartnersAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,HttpSession session) {
        LOGGER.info("CMS PARTNER CUSTOM QUERY:: {}",customeQuery);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return cmsPartnerRepository.queryCMSPartnersAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @PostMapping("/addCMSPartner")
    @ResponseBody
    public JsonResponse addCMSPartner(@Valid CMSPartnerForm request, BindingResult bindingResult, HttpSession session) {
        JsonResponse response = new JsonResponse();
        LOGGER.info("CMS Partner registration ... {}", request.toString());
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> {
                                return error1;
                            })
                    );
            response.setStatus("FAIL");
            response.setResult(errors);
        } else {
            if (1 == this.cmsPartnerRepository.saveCMSPartner(request)) {
                response.setStatus("SUCCESS");
                response.setResult("Successfully save into the database");
            } else {
                response.setStatus("ERROR");
                response.setResult("failed to submit data into the database");
            }
        }
        return response;
    }


    @GetMapping("/fireGetPartnerAccount")
    public String fireFetchAccByControlNo(Model model){
        model.addAttribute("pageTitle","GET PARTNER ACCOUNT USING CONTROL NUMBER");
        return "/itax/portal_admin/getAccByControlNo";
    }


    @RequestMapping(value = "/fireGetPartnerAccountAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ItaxJsonResponse getPartnerAccountAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String controlNo = customeQuery.get("controlNo");
        List<Map<String, Object>> finalResult = null;

        ItaxJsonResponse itaxJsonResponse = new ItaxJsonResponse();

        finalResult = cmsPartnerRepository.getAccByControlNoAjax(controlNo);
        itaxJsonResponse.setStatus("SUCCESS");
        itaxJsonResponse.setResult(finalResult);
        return itaxJsonResponse;
    }
}
