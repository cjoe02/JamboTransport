package com.example.jambo;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ArrivalsAdapter extends RecyclerView.Adapter<ArrivalsAdapter.ArrivalViewHolder> {

    private List<Arrival> arrivals = new ArrayList<>();
    private OnArrivalClickListener clickListener;

    public interface OnArrivalClickListener {
        void onArrivalClick(Arrival arrival);
    }

    public void setOnArrivalClickListener(OnArrivalClickListener listener) {
        this.clickListener = listener;
    }

    public void setArrivals(List<Arrival> arrivals) {
        this.arrivals = arrivals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArrivalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_arrival, parent, false);
        return new ArrivalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArrivalViewHolder holder, int position) {
        Arrival arrival = arrivals.get(position);
        holder.routeName.setText(arrival.getRouteName());
        holder.headsign.setText(arrival.getHeadsign());
        holder.busLabel.setText(arrival.getBusLabel());
        holder.arrivalTime.setText(arrival.getArrivalTime());

        holder.itemView.setOnClickListener(v -> {
            Log.d("ArrivalsAdapter", "Item clicked: " + arrival.getBusLabel());
            if (clickListener != null) {
                Log.d("ArrivalsAdapter", "Calling click listener");
                clickListener.onArrivalClick(arrival);
            } else {
                Log.d("ArrivalsAdapter", "Click listener is null!");
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrivals.size();
    }

    static class ArrivalViewHolder extends RecyclerView.ViewHolder {
        TextView routeName;
        TextView headsign;
        TextView busLabel;
        TextView arrivalTime;

        public ArrivalViewHolder(@NonNull View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.route_name);
            headsign = itemView.findViewById(R.id.headsign);
            busLabel = itemView.findViewById(R.id.trip_id);
            arrivalTime = itemView.findViewById(R.id.arrival_time);
        }
    }
}
