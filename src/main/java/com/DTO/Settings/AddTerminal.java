package com.DTO.Settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import philae.api.CnAccount;

@Getter
@Setter
@ToString
public class AddTerminal {
    public String location;
    public String operator;
    public String scheme;
    public String status;
    public String sysUser;
    public String terminalId;
    public CnAccount account;
}
