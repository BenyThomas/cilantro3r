package com.entities;

import lombok.*;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@NoArgsConstructor
@ToString
public class QueryInfo {
    private String endRecId;
    private String itemCount;
    private String startRecId;
}
