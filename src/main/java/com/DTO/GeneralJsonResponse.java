package com.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class GeneralJsonResponse {
    private String status = null;
    private Object result = null;
}
