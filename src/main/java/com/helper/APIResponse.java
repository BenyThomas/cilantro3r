package com.helper;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class APIResponse<T> {
    private boolean status;
    private String message;
    private T data;

    public APIResponse(boolean status, String message, T data){
        this.status = status;
        this.message = message;
        this.data = data;
    }
    public static <T> APIResponse<T> ok(T data) { return new APIResponse<>(true,"SUCCESS", data); }
    public static <T> APIResponse<T> error(String msg) { return new APIResponse<>(false, msg, null); }
}
