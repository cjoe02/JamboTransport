package com.example.jambo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    private ExecutorService executor;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        startMapDownload();
    }
    
    private void startMapDownload() {
        executor.execute(() -> {
            try {
                downloadMajuroTiles();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    finishLoading();
                });
            }
        });
    }
    
    private void downloadMajuroTiles() {
        // Define Majuro area bounds
        double north = 7.120;
        double south = 7.095;
        double east = 171.385;
        double west = 171.360;
        
        BoundingBox majuroBounds = new BoundingBox(north, east, south, west);
        
        // Simulate tile downloading with progress updates
        int totalTiles = 100; // Approximate number of tiles needed
        
        for (int i = 0; i <= totalTiles; i++) {
            try {
                // Simulate download time
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        mainHandler.post(() -> {
            finishLoading();
        });
    }
    
    private void finishLoading() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 1000);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}