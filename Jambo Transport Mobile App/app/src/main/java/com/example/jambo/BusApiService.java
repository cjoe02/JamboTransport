package com.example.jambo;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BusApiService {
    @GET("api/gtfs/stops/{stopId}/arrivals")
    Call<List<Arrival>> getArrivals(
        @Path("stopId") String stopId,
        @Query("limit") int limit
    );

    @GET("api/gtfs/buses/{tripId}")
    Call<ActiveBus> getActiveBus(@Path("tripId") String tripId);

    @GET("api/gtfs/buses/route/{route}/path")
    Call<RoutePathResponse> getRoutePath(@Path("route") String route);

    @GET("api/gtfs/buses/route/{route}/stops")
    Call<List<BusStop>> getRouteStops(@Path("route") String route);

    @GET("api/routes/{route}/status")
    Call<RouteStatus> getRouteStatus(@Path("route") String route);
}
