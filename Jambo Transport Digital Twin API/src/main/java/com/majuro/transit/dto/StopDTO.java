package com.majuro.transit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StopDTO {

    private String id;
    private String name;
    private Double latitude;
    private Double longitude;
}
