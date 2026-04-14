package com.SMARTLIFE.curator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.SMARTLIFE.curator.databinding.ActivitySchedule3Binding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

public class ScheduleActivity3 extends AppCompatActivity implements OnMapReadyCallback {

    private ActivitySchedule3Binding binding;
    private GoogleMap mMap;
    private String currentCity = "Chennai";
    private LatLng destinationLatLng;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location originLocation;

    private static final int LOCATION_REQUEST_CODE = 1001;

    // 🔥 FIRESTORE
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySchedule3Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Fix handshake issues
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (Exception e) {
            Log.e("SECURITY", "ProviderInstaller failed: " + e.getMessage());
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();
        db = FirebaseFirestore.getInstance();

        currentCity = getIntent().getStringExtra("DESTINATION");
        if (currentCity == null || currentCity.isEmpty()) currentCity = "Chennai";

        binding.etDestination.setText(currentCity);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        fetchWeatherData(currentCity);

        // Show image initially
        binding.mapOverlayImage.setVisibility(View.VISIBLE);

        // 🔥 BUTTON CLICK - Updated to only update map and weather
        binding.btnShowMap.setOnClickListener(v -> {
            String location = binding.etDestination.getText().toString().trim();

            if (!location.isEmpty()) {
                currentCity = location;
                fetchWeatherData(currentCity);
                
                // Note: We are no longer hiding the mapOverlayImage here
                // binding.mapOverlayImage.setVisibility(View.GONE);

                Toast.makeText(this, "Showing for " + currentCity, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Enter destination", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔥 LOAD TASKS
        loadTasks();

        binding.btnOpenMaps.setOnClickListener(v -> {
            Uri uri = Uri.parse("google.navigation:q=" + currentCity);
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        });
        
        // Navigation Bar Listeners
        binding.navHomeContainer.setOnClickListener(v -> finish());
        binding.navProfileContainer.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity3.class));
            finish();
        });
    }

    private void setupLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    originLocation = location;
                    refreshMapGraphics();
                }
            }
        };
    }

    private void refreshMapGraphics() {
        if (mMap == null) return;
        
        mMap.clear();

        if (destinationLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title("Destination: " + currentCity)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        if (originLocation != null) {
            LatLng originLatLng = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(originLatLng)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            if (destinationLatLng != null) {
                mMap.addPolyline(new PolylineOptions()
                        .add(originLatLng, destinationLatLng)
                        .width(12)
                        .color(Color.BLUE)
                        .geodesic(true));
                
                LatLngBounds bounds = new LatLngBounds.Builder()
                        .include(originLatLng)
                        .include(destinationLatLng)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            }
        } else if (destinationLatLng != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12));
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void loadTasks() {
        db.collection("users")
                .document(userId)
                .collection("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        String location = doc.getString("location");
                        Log.d("TASK", "Title: " + title + ", Location: " + location);
                    }
                });
    }

    private void fetchWeatherData(String city) {
        new GeocodeTask(city).execute();
    }

    private class GeocodeTask extends AsyncTask<Void, Void, LatLng> {
        String city;
        GeocodeTask(String city) { this.city = city; }
        protected LatLng doInBackground(Void... v) {
            try {
                Geocoder g = new Geocoder(ScheduleActivity3.this);
                List<Address> list = g.getFromLocationName(city, 1);
                if (list != null && !list.isEmpty()) {
                    return new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
                }
            } catch (Exception e) {
                Log.e("GEOCODE", "Error geocoding city: " + e.getMessage());
            }
            return null;
        }
        protected void onPostExecute(LatLng res) {
            if (res != null) {
                destinationLatLng = res;
                new WeatherTask().execute(res.latitude, res.longitude);
                refreshMapGraphics();
            } else {
                Toast.makeText(ScheduleActivity3.this, "Could not find coordinates for " + city, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class WeatherTask extends AsyncTask<Double, Void, String> {
        private String errorDetail = null;

        protected String doInBackground(Double... c) {
            HttpsURLConnection conn = null;
            try {
                String urlString = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true&daily=weather_code,temperature_2m_max&timezone=auto",
                    c[0], c[1]);
                Log.d("WEATHER", "Requesting: " + urlString);

                URL url = new URL(urlString);
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000); 
                conn.setReadTimeout(15000);
                
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorDetail = "Server Error: " + responseCode;
                    return null;
                }

                InputStream inputStream = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return sb.toString();
            } catch (Exception e) {
                Log.e("WEATHER", "Error fetching weather", e);
                errorDetail = e.getMessage();
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        protected void onPostExecute(String r) {
            if (r != null) {
                try {
                    JSONObject json = new JSONObject(r);
                    
                    // Update REAL-TIME Current Weather
                    if (json.has("current_weather")) {
                        JSONObject current = json.getJSONObject("current_weather");
                        double currentTemp = current.getDouble("temperature");
                        int weatherCode = current.getInt("weathercode");
                        
                        binding.tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f°C", currentTemp));
                        binding.tvCurrentCondition.setText(getWeatherDescription(weatherCode));
                        setWeatherIcon(binding.ivCurrentWeather, weatherCode);
                    }

                    // Update 7-Day Forecast
                    if (json.has("daily")) {
                        JSONObject daily = json.getJSONObject("daily");
                        JSONArray times = daily.getJSONArray("time");
                        JSONArray codes = daily.getJSONArray("weather_code");
                        JSONArray temps = daily.getJSONArray("temperature_2m_max");

                        updateForecastUI(times, codes, temps);
                    }
                } catch (Exception e) {
                    Log.e("WEATHER", "JSON Parse Error: " + e.getMessage());
                }
            } else {
                String msg = (errorDetail != null) ? "Weather Error: " + errorDetail : "Failed to fetch weather data";
                Toast.makeText(ScheduleActivity3.this, msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateForecastUI(JSONArray times, JSONArray codes, JSONArray temps) throws Exception {
        TextView[] dayViews = {binding.tvDay1, binding.tvDay2, binding.tvDay3, binding.tvDay4, binding.tvDay5, binding.tvDay6, binding.tvDay7};
        TextView[] tempViews = {binding.tvTemp1, binding.tvTemp2, binding.tvTemp3, binding.tvTemp4, binding.tvTemp5, binding.tvTemp6, binding.tvTemp7};
        ImageView[] iconViews = {binding.ivWeather1, binding.ivWeather2, binding.ivWeather3, binding.ivWeather4, binding.ivWeather5, binding.ivWeather6, binding.ivWeather7};

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            if (i < times.length()) {
                Date date = inputFormat.parse(times.getString(i));
                dayViews[i].setText(outputFormat.format(date));
                tempViews[i].setText(String.format(Locale.getDefault(), "%.0f°C", temps.getDouble(i)));
                setWeatherIcon(iconViews[i], codes.getInt(i));
            }
        }
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Clear sky";
        if (code == 1 || code == 2 || code == 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 55) return "Drizzle";
        if (code >= 61 && code <= 65) return "Rainy";
        if (code >= 71 && code <= 75) return "Snowy";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 95) return "Thunderstorm";
        return "Overcast";
    }

    private void setWeatherIcon(ImageView iv, int code) {
        int icon = android.R.drawable.ic_menu_day; // Default sun icon
        if (code >= 1 && code <= 3) icon = android.R.drawable.ic_menu_report_image;
        else if (code >= 61 && code <= 82) icon = android.R.drawable.ic_menu_send;
        else if (code >= 95) icon = android.R.drawable.ic_menu_compass;

        iv.setImageResource(icon);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        refreshMapGraphics();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}
