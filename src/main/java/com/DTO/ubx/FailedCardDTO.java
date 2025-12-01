package com.DTO.ubx;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FailedCardDTO {
    private Long cardId;
    private String pan;
    private String accountNo;
    private String customerName;
    private String branch;
    private String failedStep;
    private String failureMessage;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime failedAt;
}

