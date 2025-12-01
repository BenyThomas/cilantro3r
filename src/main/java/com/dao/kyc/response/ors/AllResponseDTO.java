package com.dao.kyc.response.ors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllResponseDTO<A,C,D>{
    private A attachments;
    private C classifiers;
    private D entity;


}
