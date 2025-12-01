package com.DTO.psssf;



import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author melleji.mollel
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanRepaymentResp {

    private String responseCode;
    private String message;
    private String reference;

}
