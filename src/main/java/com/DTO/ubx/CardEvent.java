package com.DTO.ubx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardEvent {
    private String cardId;
    private String account;
    private String status;
    private int retryCount;
}