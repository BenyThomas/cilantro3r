package com.DTO.Teller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.beans.Transient;
import java.util.List;
import java.util.Map;
@Setter
@Getter
public class MobileMovement {
    @NotBlank(message = "Indicator is required")
    private String indicator;
    @NotBlank(message = "Amount is required")
    private String amount;
    @NotBlank(message = "Receipt is required")
    private String reference;
    @NotBlank(message = "Transaction narrations is required")
    private String comments;
    @NotBlank(message = "Account is required")
    private String sourceAcctDestAcct;

}
