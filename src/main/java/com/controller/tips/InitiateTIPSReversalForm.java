package com.controller.tips;

import javax.validation.constraints.NotEmpty;

public class InitiateTIPSReversalForm {
    @NotEmpty(message = "Reversal reason is required")
    private String reverseTxnReason;
}
