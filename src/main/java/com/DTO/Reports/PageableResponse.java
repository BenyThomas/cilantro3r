package com.DTO.Reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageableResponse {
    private SortResponse sort;
    private int offset;
    private int pageSize;
    private int pageNumber;
    private boolean unpaged;
    private boolean paged;
}
