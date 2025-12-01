package com.dao.kyc.response.zcsra;

import lombok.Data;

@Data
public class ResponsePayload {
    private ResponseData data;
    private int code;
    private String msg;
}
