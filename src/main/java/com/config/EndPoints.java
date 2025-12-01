package com.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndPoints {
    @Value("${brela.ors.base.url}")
    private String BRELA_ORS_BASE_URL;
}
