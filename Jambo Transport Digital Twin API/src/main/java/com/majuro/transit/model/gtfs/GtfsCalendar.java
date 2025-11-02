package com.majuro.transit.model.gtfs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "gtfs_calendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsCalendar {

    @Id
    private String serviceId;

    @Column(nullable = false)
    private Boolean monday;

    @Column(nullable = false)
    private Boolean tuesday;

    @Column(nullable = false)
    private Boolean wednesday;

    @Column(nullable = false)
    private Boolean thursday;

    @Column(nullable = false)
    private Boolean friday;

    @Column(nullable = false)
    private Boolean saturday;

    @Column(nullable = false)
    private Boolean sunday;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
}
