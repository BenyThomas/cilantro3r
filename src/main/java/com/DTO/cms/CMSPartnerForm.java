package com.DTO.cms;

import jakarta.validation.constraints.NotBlank;

public class CMSPartnerForm {

    @NotBlank(message = "Partner name is required")
    private String partner_name;

    @NotBlank(message = "Account number is required")
    private String acct_no;


    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotBlank(message = "Partner type is required")
    private String partner_type;

    @NotBlank(message = "Partner code is required")
    private String partner_code;

    @NotBlank(message = "Bot account is required")
    private String bot_acct;

    public String getPartner_name() {
        return partner_name;
    }

    public void setPartner_name(String partner_name) {
        this.partner_name = partner_name;
    }

    public String getAcct_no() {
        return acct_no;
    }

    public void setAcct_no(String acct_no) {
        this.acct_no = acct_no;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getPartner_type() {
        return partner_type;
    }

    public void setPartner_type(String partner_type) {
        this.partner_type = partner_type;
    }

    public String getPartner_code() {
        return partner_code;
    }

    public void setPartner_code(String partner_code) {
        this.partner_code = partner_code;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBot_acct() {
        return bot_acct;
    }

    public void setBot_acct(String bot_acct) {
        this.bot_acct = bot_acct;
    }

    @Override
    public String toString() {
        return "CMSPartnerForm{" +
                "partner_name='" + partner_name + '\'' +
                ", acct_no='" + acct_no + '\'' +
                ", currency='" + currency + '\'' +
                ", purpose='" + purpose + '\'' +
                ", partner_type='" + partner_type + '\'' +
                ", partner_code='" + partner_code + '\'' +
                ", bot_acct='" + bot_acct + '\'' +
                '}';
    }
}
