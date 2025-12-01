package com.DTO.Teller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class VikobaFundMovement {
    @NotBlank(message = "Update Control is required")
    private String updateControl;
    @NotBlank(message = "Mno field is required")
    private String mno;
    @NotBlank(message = "Amount is required")
    private String amount;
    @NotBlank(message = "Mno receipt is required")
    private String mnoReceipt;
}
