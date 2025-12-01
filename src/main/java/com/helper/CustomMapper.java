package com.helper;

import com.DTO.KYC.ors.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.models.kyc.ors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomMapper {
    private final ObjectMapper mapper;
}
