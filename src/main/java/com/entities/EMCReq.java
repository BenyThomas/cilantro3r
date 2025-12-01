package com.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

/**
 *
 * @author arthur.ndossi
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class EMCReq {
    private String acctNo;
    @JsonIgnore
    private String branchCode;
    @JsonIgnore
    private String branchId;
    @JsonIgnore
    private String branchName;
    private String customerNumber;
    @JsonIgnore
    private String accessCode;
}
