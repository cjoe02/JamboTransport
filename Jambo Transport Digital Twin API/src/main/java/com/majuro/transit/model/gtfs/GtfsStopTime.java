package com.majuro.transit.model.gtfs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "gtfs_stop_times")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsStopTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private GtfsTrip trip;

    @ManyToOne
    @JoinColumn(name = "stop_id", nullable = false)
    private GtfsStop stop;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private Integer stopSequence;
}
