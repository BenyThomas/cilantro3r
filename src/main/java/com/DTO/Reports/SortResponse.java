package com.DTO.Reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SortResponse {
    private boolean empty;
    private boolean sorted;
    private boolean unsorted;
}
