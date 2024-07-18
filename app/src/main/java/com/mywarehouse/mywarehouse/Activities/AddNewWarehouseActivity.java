package com.mywarehouse.mywarehouse.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.CustomNestedScrollView;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddNewWarehouseActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng currentLatLng;
    private BottomNavigationView bottomNavigationView;
    private MaterialButton saveButton;
    private TextInputEditText searchInput, inputName;
    private List<Marker> markers = new ArrayList<>();
    private Polygon currentPolygon;
    private CustomNestedScrollView customNestedScrollView;
    private FirebaseFirestore db;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_warehouse);

        searchInput = findViewById(R.id.search_input);
        inputName = findViewById(R.id.input_name);
        saveButton = findViewById(R.id.button_save_warehouse);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        customNestedScrollView = findViewById(R.id.custom_nested_scroll_view);
        db = FirebaseFirestore.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        // Initialize the FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up save button
        saveButton.setOnClickListener(v -> checkAndSaveWarehouse());

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 3) {
                    searchLocation(s.toString());
                } else if (s.length() == 0) {
                    getDeviceLocation();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set the map to hibernate mode
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getDeviceLocation();
        }

        // Enable the user's location on the map
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Add map click listener to add markers
        mMap.setOnMapClickListener(latLng -> {
            if (markers.size() < 4) {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_marker))
                        .title("Search Result"));
                markers.add(marker);
                if (markers.size() == 4) {
                    sortAndDrawWarehouse();
                }
            } else {
                Toast.makeText(this, "Maximum of 4 points allowed", Toast.LENGTH_SHORT).show();
            }
        });

        // Add marker click listener to remove markers
        mMap.setOnMarkerClickListener(marker -> {
            marker.remove();
            markers.remove(marker);
            if (currentPolygon != null) {
                currentPolygon.remove();
                currentPolygon = null;
            }
            return true;
        });

        // Disable scroll view while interacting with the map
        mMap.setOnCameraMoveStartedListener(reason -> customNestedScrollView.setScrollingEnabled(false));
        mMap.setOnCameraIdleListener(() -> customNestedScrollView.setScrollingEnabled(true));
    }

    private void getDeviceLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
                    }
                });
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "SecurityException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sortAndDrawWarehouse() {
        if (markers.size() != 4) {
            Toast.makeText(this, "Please add exactly 4 points", Toast.LENGTH_SHORT).show();
            return;
        }

        List<LatLng> points = new ArrayList<>();
        for (Marker marker : markers) {
            points.add(marker.getPosition());
        }

        List<LatLng> sortedPoints = sortPoints(points);

        if (currentPolygon != null) {
            currentPolygon.remove();
        }

        PolygonOptions polygonOptions = new PolygonOptions();
        for (LatLng point : sortedPoints) {
            polygonOptions.add(point);
        }

        currentPolygon = mMap.addPolygon(polygonOptions);
    }

    public List<LatLng> sortPoints(List<LatLng> points) {
        if (points.size() != 4) {
            throw new IllegalArgumentException("There must be exactly 4 points.");
        }

        // Find the bottom-left point
        LatLng bottomLeft = Collections.min(points, new Comparator<LatLng>() {
            @Override
            public int compare(LatLng p1, LatLng p2) {
                if (p1.latitude != p2.latitude) {
                    return Double.compare(p1.latitude, p2.latitude);
                } else {
                    return Double.compare(p1.longitude, p2.longitude);
                }
            }
        });

        // Remove the bottom-left point from the list
        points.remove(bottomLeft);

        // Sort the remaining points based on their positions relative to the bottom-left point
        points.sort(new Comparator<LatLng>() {
            @Override
            public int compare(LatLng p1, LatLng p2) {
                double angle1 = Math.atan2(p1.latitude - bottomLeft.latitude, p1.longitude - bottomLeft.longitude);
                double angle2 = Math.atan2(p2.latitude - bottomLeft.latitude, p2.longitude - bottomLeft.longitude);
                return Double.compare(angle1, angle2);
            }
        });

        // Add the bottom-left point back to the beginning of the list
        points.add(0, bottomLeft);

        return points;
    }
    private void checkAndSaveWarehouse() {
        String name = inputName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name for the warehouse", Toast.LENGTH_SHORT).show();
            return;
        }
        if (markers.size() != 4)
        {
            Toast.makeText(this, "Please draw a rectangle with exactly 4 points", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("warehouses")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Warehouse name already exists. Please choose a different name.", Toast.LENGTH_SHORT).show();
                    } else {
                        saveWarehouse(name);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking warehouse name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveWarehouse(String name) {

            List<LatLng> points = new ArrayList<>();
            for (Marker marker : markers) {
                points.add(marker.getPosition());
            }

            Warehouse warehouse = new Warehouse(name, points,true);
            db.collection("warehouses").add(warehouse)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Warehouse saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving warehouse: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }


    private void setupBottomNavigation() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent = null;
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                intent = new Intent(AddNewWarehouseActivity.this, HomeActivity.class);
            } else if (id == R.id.navigation_account) {
                intent = new Intent(AddNewWarehouseActivity.this, AccountActivity.class);
            } else if (id == R.id.navigation_reports) {
                intent = new Intent(AddNewWarehouseActivity.this, ReportsActivity.class);
            } else if (id == R.id.navigation_orders) {
                intent = new Intent(AddNewWarehouseActivity.this, OrdersActivity.class);
            } else if (id == R.id.navigation_inventory) {
                intent = new Intent(AddNewWarehouseActivity.this, InventoryActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                startActivity(intent);
                finish();
            }
            return true;
        });
    }

    private void searchLocation(String query) {
        executorService.submit(() -> {
            try {
                String urlString = "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=1";
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                String response = stringBuilder.toString();
                JSONArray jsonArray = new JSONArray(response);
                if (jsonArray.length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");
                    LatLng latLng = new LatLng(lat, lon);
                    runOnUiThread(() -> {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(AddNewWarehouseActivity.this, "Location not found", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> Toast.makeText(AddNewWarehouseActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
