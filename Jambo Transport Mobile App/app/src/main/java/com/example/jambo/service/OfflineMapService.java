package com.example.jambo.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.Counters;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineMapService {
    private static final String TAG = "OfflineMapService";
    private static OfflineMapService instance;
    private Context context;
    private ExecutorService executor;
    
    public interface DownloadCallback {
        void onProgress(int tilesDownloaded, int totalTiles);
        void onComplete();
        void onError(String error);
    }
    
    private OfflineMapService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static OfflineMapService getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineMapService(context);
        }
        return instance;
    }
    
    public void downloadMajuroArea(DownloadCallback callback) {
        executor.execute(() -> {
            try {
                BoundingBox majuroBounds = getMajuroBoundingBox();
                int minZoom = 10;
                int maxZoom = 16;
                
                Handler mainHandler = new Handler(Looper.getMainLooper());
                
                downloadTilesForArea(majuroBounds, minZoom, maxZoom, new DownloadCallback() {
                    @Override
                    public void onProgress(int tilesDownloaded, int totalTiles) {
                        mainHandler.post(() -> callback.onProgress(tilesDownloaded, totalTiles));
                    }
                    
                    @Override
                    public void onComplete() {
                        mainHandler.post(callback::onComplete);
                    }
                    
                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> callback.onError(error));
                    }
                });
                
            } catch (Exception e) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> callback.onError("Download failed: " + e.getMessage()));
            }
        });
    }
    
    private BoundingBox getMajuroBoundingBox() {
        double north = 7.12;
        double south = 7.10;
        double east = 171.20;
        double west = 171.07;
        
        return new BoundingBox(north, east, south, west);
    }
    
    private void downloadTilesForArea(BoundingBox boundingBox, int minZoom, int maxZoom, DownloadCallback callback) {
        try {
            int totalTiles = 0;
            int downloadedTiles = 0;
            
            // Calculate total tiles
            for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
                int tilesAtZoom = calculateTilesInBoundingBox(boundingBox, zoom);
                totalTiles += tilesAtZoom;
            }
            
            Log.d(TAG, "Total tiles to download: " + totalTiles);
            
            // Simulate download progress (in real implementation, you'd download actual tiles)
            for (int i = 0; i <= totalTiles; i += Math.max(1, totalTiles / 20)) {
                try {
                    Thread.sleep(100); // Simulate download time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                final int progress = i;
                callback.onProgress(Math.min(progress, totalTiles), totalTiles);
            }
            
            // Create cache directory structure
            createCacheDirectories();
            
            callback.onComplete();
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading tiles", e);
            callback.onError("Download error: " + e.getMessage());
        }
    }
    
    private int calculateTilesInBoundingBox(BoundingBox boundingBox, int zoom) {
        // Simplified calculation - in reality this would be more precise
        double latDiff = boundingBox.getLatNorth() - boundingBox.getLatSouth();
        double lonDiff = boundingBox.getLonEast() - boundingBox.getLonWest();
        
        int tilesPerDegree = (int) Math.pow(2, zoom);
        int latTiles = (int) Math.ceil(latDiff * tilesPerDegree);
        int lonTiles = (int) Math.ceil(lonDiff * tilesPerDegree);
        
        return latTiles * lonTiles;
    }
    
    private void createCacheDirectories() {
        try {
            File osmDir = Configuration.getInstance().getOsmdroidTileCache();
            if (!osmDir.exists()) {
                osmDir.mkdirs();
            }
            
            File mapnikDir = new File(osmDir, "Mapnik");
            if (!mapnikDir.exists()) {
                mapnikDir.mkdirs();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create cache directories", e);
        }
    }
    
    public boolean isOfflineDataAvailable() {
        try {
            File osmDir = Configuration.getInstance().getOsmdroidTileCache();
            File mapnikDir = new File(osmDir, "Mapnik");
            
            return mapnikDir.exists() && mapnikDir.listFiles() != null && mapnikDir.listFiles().length > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check offline data availability", e);
            return false;
        }
    }
    
    public void configureOfflineMap(MapView mapView) {
        if (isOfflineDataAvailable()) {
            Log.d(TAG, "Offline data available, using cached tiles");
            
            // Enable offline mode
            Configuration.getInstance().setUserAgentValue("MajuroTransit/1.0");
            
            // The default tile provider will automatically use cached tiles when available
            mapView.setTileSource(TileSourceFactory.MAPNIK);
        }
    }
    
    public long getCacheSize() {
        try {
            File osmDir = Configuration.getInstance().getOsmdroidTileCache();
            return getFolderSize(osmDir);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get cache size", e);
            return 0;
        }
    }
    
    private long getFolderSize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getFolderSize(file);
                    }
                }
            }
        }
        return size;
    }
    
    public String getCacheSizeFormatted() {
        long bytes = getCacheSize();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}