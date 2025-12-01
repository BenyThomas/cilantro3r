package com.DTO.Teller;

public class CustomJsonResponse {
    private int code;
    private Object data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CustomJsonResponse{" +
                "status='" + code + '\'' +
                ", data=" + data +
                '}';
    }
}
