package com.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class GetmnoBalancesResponse {
    public int responseCode;
    public String responseMessage;
    public String collectionBalance;
    public String collectionBalanceLastUpdate;
    public String disbursementBalance;
    public String disbursementBalanceLastUpdate;
}