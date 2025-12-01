package com.DTO.Reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageableContent<T> {
    private List<T> content;
    private PageableResponse pageable;
    private boolean last;
    private int totalPages;
    private int totalElements;
    private int size;
    private int number;
    private boolean first;
    private int numberOfElements;
    private boolean empty;
}
