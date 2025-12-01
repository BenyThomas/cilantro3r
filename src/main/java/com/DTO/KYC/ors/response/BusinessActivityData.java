package com.DTO.KYC.ors.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessActivityData {
    @JsonProperty("name_of_activity_category")
    private String nameOfActivityCategory;
    @JsonProperty("name_of_activity_division")
    private String nameOfActivityDivision;
    @JsonProperty("name_of_activity_group")
    private String nameOfActivityGroup;
    @JsonProperty("name_of_activity_class")
    private String nameOfActivityClass;
    @JsonProperty("main_activity")
    private String mainActivity;
}
