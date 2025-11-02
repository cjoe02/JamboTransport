package com.example.jambo;

import android.view.View;
import android.widget.TextView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class CustomInfoWindow extends InfoWindow {

    private OnInfoWindowClickListener clickListener;

    public interface OnInfoWindowClickListener {
        void onInfoWindowClick();
    }

    public CustomInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
    }

    public void setOnInfoWindowClickListener(OnInfoWindowClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;

        TextView title = mView.findViewById(R.id.bubble_title);
        TextView description = mView.findViewById(R.id.bubble_description);

        if (title != null) {
            title.setText(marker.getTitle());
        }

        if (description != null) {
            description.setText(marker.getSnippet());
        }

        mView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onInfoWindowClick();
            }
        });
    }

    @Override
    public void onClose() {
        // Nothing to do
    }
}
