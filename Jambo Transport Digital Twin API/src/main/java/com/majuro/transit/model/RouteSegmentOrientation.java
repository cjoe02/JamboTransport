package com.majuro.transit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegmentOrientation {

    private String routeId;
    private String fromStop;
    private String toStop;
    private Orientation orientation;

    public enum Orientation {
        NORTH_FACING,  // Exposed to northerly waves (270-90 degrees)
        SOUTH_FACING   // Exposed to southerly waves (90-270 degrees)
    }
}
