package com.DTO.ubx;


import lombok.Data;
import java.util.List;

@Data
public class InstantCardDetails {
    private InstantCardSummary card;
    private List<EventTrail> events;
}

