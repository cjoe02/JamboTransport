package com.example.jambo.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jambo.ActiveBus;
import com.example.jambo.Arrival;
import com.example.jambo.BusApiService;
import com.example.jambo.BusStop;
import com.example.jambo.RoutePathResponse;
import com.example.jambo.RouteShape;
import com.example.jambo.service.OsrmService;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteRepository {

    private final BusApiService apiService;
    private final OsrmService osrmService;
    private final ExecutorService executorService;
    private final MutableLiveData<List<BusStop>> busStopsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<BusStop>> routeStopsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Arrival>> arrivalsLiveData = new MutableLiveData<>();
    private final MutableLiveData<ActiveBus> activeBusLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<RouteShape>> routePathLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public RouteRepository(BusApiService apiService) {
        this.apiService = apiService;
        this.osrmService = new OsrmService();
        this.executorService = Executors.newSingleThreadExecutor();
        initializeBusStops();
    }

    private void initializeBusStops() {
        List<BusStop> busStops = new ArrayList<>();
        busStops.add(new BusStop("rm_alw", "Alwal", 7.124234, 171.356106));
        busStops.add(new BusStop("rm_lkn", "Lokonmok", 7.123838, 171.359397));
        busStops.add(new BusStop("rm_rcn", "RCS North", 7.122994, 171.360882));
        busStops.add(new BusStop("rm_rcs", "RCS South", 7.122318, 171.361692));
        busStops.add(new BusStop("rm_res", "RES", 7.121043, 171.362946));
        busStops.add(new BusStop("rm_mhn", "MIHS North", 7.119786, 171.364358));
        busStops.add(new BusStop("rm_mih", "MIHS", 7.118617, 171.365009));
        busStops.add(new BusStop("rm_mhs", "MIHS South", 7.11657, 171.365814));
        busStops.add(new BusStop("rm_utr", "Utrikan", 7.115459, 171.366239));
        busStops.add(new BusStop("rm_taf", "Track and Field", 7.111589, 171.368419));
        busStops.add(new BusStop("rm_mie", "Mieco", 7.110391, 171.369723));
        busStops.add(new BusStop("dud_dtn", "Downtown Uliga", 7.109348, 171.371379));
        busStops.add(new BusStop("rb_anm", "Anmarwut", 7.12533, 171.358007));
        busStops.add(new BusStop("rb_lkb", "Lokonmok Back", 7.125099, 171.360498));
        busStops.add(new BusStop("rb_rpn", "Rita Protestant North", 7.124492, 171.36214));
        busStops.add(new BusStop("rb_rps", "Rita Protestant South", 7.123609, 171.363288));
        busStops.add(new BusStop("rb_mbc", "MBCA", 7.122022, 171.364516));

        busStopsLiveData.setValue(busStops);
    }

    public LiveData<List<BusStop>> getBusStops() {
        return busStopsLiveData;
    }

    public LiveData<List<BusStop>> getRouteStops() {
        return routeStopsLiveData;
    }

    public LiveData<List<Arrival>> getArrivals() {
        return arrivalsLiveData;
    }

    public LiveData<ActiveBus> getActiveBus() {
        return activeBusLiveData;
    }

    public LiveData<List<RouteShape>> getRoutePath() {
        return routePathLiveData;
    }

    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void fetchArrivals(String stopId, int limit) {
        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        apiService.getArrivals(stopId, limit).enqueue(new Callback<List<Arrival>>() {
            @Override
            public void onResponse(Call<List<Arrival>> call, Response<List<Arrival>> response) {
                loadingLiveData.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Arrival> arrivals = response.body();
                    arrivalsLiveData.setValue(arrivals);
                } else {
                    errorLiveData.setValue("Failed to load arrivals");
                }
            }

            @Override
            public void onFailure(Call<List<Arrival>> call, Throwable t) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue("Unable to connect to server");
            }
        });
    }

    public void fetchActiveBus(String tripId) {
        apiService.getActiveBus(tripId).enqueue(new Callback<ActiveBus>() {
            @Override
            public void onResponse(Call<ActiveBus> call, Response<ActiveBus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ActiveBus bus = response.body();
                    activeBusLiveData.setValue(bus);
                }
            }
            @Override
            public void onFailure(Call<ActiveBus> call, Throwable t) {
            }
        });
    }

    public void clearArrivals() {
        arrivalsLiveData.setValue(null);
    }

    public void clearActiveBus() {
        activeBusLiveData.setValue(null);
    }

    public void fetchRoutePath(String route) {
        apiService.getRoutePath(route).enqueue(new Callback<RoutePathResponse>() {
            @Override
            public void onResponse(Call<RoutePathResponse> call, Response<RoutePathResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RoutePathResponse pathResponse = response.body();
                    List<RouteShape> path = pathResponse.getPath();
                    if (path != null && !path.isEmpty()) {
                        // Use original path with stopIds - don't use OSRM which strips stop data
                        routePathLiveData.setValue(path);
                    }
                }
            }
            @Override
            public void onFailure(Call<RoutePathResponse> call, Throwable t) {
            }
        });
    }

    public void fetchRouteStops(String route) {
        apiService.getRouteStops(route).enqueue(new Callback<List<BusStop>>() {
            @Override
            public void onResponse(Call<List<BusStop>> call, Response<List<BusStop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BusStop> stops = response.body();
                    routeStopsLiveData.setValue(stops);
                } else {
                    routeStopsLiveData.setValue(new ArrayList<>());
                }
            }
            @Override
            public void onFailure(Call<List<BusStop>> call, Throwable t) {
                routeStopsLiveData.setValue(new ArrayList<>());
            }
        });
    }

    public void clearRouteStops() {
        routeStopsLiveData.setValue(null);
    }
}
