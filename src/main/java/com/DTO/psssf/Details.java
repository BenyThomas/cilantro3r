package com.DTO.psssf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Details {
    @JsonProperty("PENSIONER_ID")
    public String pensioner_id;
    @JsonProperty("SSN")
    public String ssn;
    @JsonProperty("FULLNAME")
    public String fullname;
    @JsonProperty("DOB")
    public String dob;
    @JsonProperty("RETIREMENT_DATE")
    public String retirement_date;
    @JsonProperty("MP_AMOUNT")
    public String mp_amount;
    @JsonProperty("BANK_ACCOUNT")
    public String bank_account;
    @JsonProperty("BANK_NAME")
    public String bank_name;
    @JsonProperty("PENSIONER_TYPE")
    public String pensioner_type;
    @JsonProperty("PENSIONER_STATUS")
    public String pensioner_status;
    @JsonProperty("STATUS_REASON")
    public Object status_reason;
    @JsonProperty("PENSIONER_END_DATE")
    public Object pensioner_end_date;
    @JsonProperty("LAST_PAYMENT")
    public String last_payment;
    @JsonProperty("HAS_LOAN")
    public String has_loan;
    @JsonProperty("PHONE")
    public String phone;
    @JsonProperty("PENSION_MODE")
    public String pension_mode;
    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
