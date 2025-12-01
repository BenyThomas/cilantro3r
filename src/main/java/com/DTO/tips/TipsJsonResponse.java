package com.DTO.tips;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TipsJsonResponse {
    private String status;
    private Object data;
}
