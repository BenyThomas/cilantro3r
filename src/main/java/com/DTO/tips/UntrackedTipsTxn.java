package com.DTO.tips;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UntrackedTipsTxn {
    public String contraAccount;
    public String customerAccount;
    public String amount;
    public String reference;
    public String descriptions;
    public String currency;
}
