package com.controller.itax.GePG;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class EgaControllerReprocess {

    @RequestMapping("/fireReprocessTransaction")
    public String validateReprocessTransactions(){
        return "/itax/GePG/reprocessValidateCtrlNo";
    }
}
