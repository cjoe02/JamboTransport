package com.example.jambo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.jambo.R;
import com.example.jambo.model.BusRoute;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {
    
    private List<BusRoute> routes;
    private OnRouteClickListener onRouteClickListener;
    
    public interface OnRouteClickListener {
        void onRouteClick(BusRoute route);
    }
    
    public RouteAdapter(List<BusRoute> routes, OnRouteClickListener listener) {
        this.routes = routes;
        this.onRouteClickListener = listener;
    }
    
    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        BusRoute route = routes.get(position);
        holder.bind(route);
    }
    
    @Override
    public int getItemCount() {
        return routes != null ? routes.size() : 0;
    }
    
    public class RouteViewHolder extends RecyclerView.ViewHolder {
        private TextView routeName;
        private TextView startLocation;
        private TextView endLocation;
        private TextView busCount;
        private TextView floodStatus;
        private View floodIndicator;
        
        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.tv_route_name);
            startLocation = itemView.findViewById(R.id.tv_start_location);
            endLocation = itemView.findViewById(R.id.tv_end_location);
            busCount = itemView.findViewById(R.id.tv_bus_count);
            floodStatus = itemView.findViewById(R.id.tv_flood_status);
            floodIndicator = itemView.findViewById(R.id.flood_indicator);
            
            itemView.setOnClickListener(v -> {
                if (onRouteClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onRouteClickListener.onRouteClick(routes.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(BusRoute route) {
            routeName.setText(route.getRouteName());
            startLocation.setText("From: " + route.getStartLocation());
            endLocation.setText("To: " + route.getEndLocation());
            
            int activeBusCount = route.getActiveBuses() != null ? route.getActiveBuses().size() : 0;
            busCount.setText(activeBusCount + " buses");
            
            floodStatus.setText(route.getFloodStatus().getDisplayName());
            
            int floodColor = getFloodStatusColor(route.getFloodStatus());
            floodStatus.setTextColor(floodColor);
            floodIndicator.setBackgroundColor(floodColor);
        }
        
        private int getFloodStatusColor(BusRoute.FloodStatus floodStatus) {
            switch (floodStatus) {
                case FLOOD_WARNING:
                    return ContextCompat.getColor(itemView.getContext(), R.color.flood_warning);
                case MINOR_FLOODING:
                    return ContextCompat.getColor(itemView.getContext(), R.color.flood_minor);
                case SAFE:
                default:
                    return ContextCompat.getColor(itemView.getContext(), R.color.flood_safe);
            }
        }
    }
}