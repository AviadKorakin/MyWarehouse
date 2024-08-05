package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.mywarehouse.mywarehouse.Adapters.WarehouseSpinnerAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseWarehouseMap;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class WarehouseMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FirebaseFirestore db;
    private Gson gson;
    private List<Item> itemList;
    private List<Warehouse> warehouseList;
    private LatLngBounds.Builder boundsBuilder;
    private boolean isMapReady = false;
    private boolean isDataLoaded = false;
    private Spinner spinnerWarehouses;
    private BottomNavigationView bottomNavigationView;
    private int warehouseIcon = R.drawable.ic_warehousemap; // Add correct icons for each warehouse
    private int outOfStockIcon = R.drawable.ic_outofstock; // Add correct icon for out of stock

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_map);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        db = FirebaseFirestore.getInstance();
        gson = new Gson();
        itemList = new ArrayList<>();
        warehouseList = new ArrayList<>();
        boundsBuilder = new LatLngBounds.Builder();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigationView();
        spinnerWarehouses = findViewById(R.id.spinner_warehouses);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        loadData();
    }

    private void loadData() {
        FirebaseWarehouseMap.loadData(db, new FirebaseWarehouseMap.DataCallback() {
            @Override
            public void onCallback(List<Item> items, List<Warehouse> warehouses) {
                itemList.clear();
                itemList.addAll(items);
                warehouseList.clear();
                warehouseList.addAll(warehouses);
                isDataLoaded = true;
                setupSpinner();
                addItemsAndWarehousesToMap();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(WarehouseMapActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupBottomNavigationView() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_inventory);
    }
    private void setupSpinner() {
        List<Warehouse> allWarehouses = new ArrayList<>();
        allWarehouses.add(new Warehouse("All", new ArrayList<>(), true));
        allWarehouses.addAll(warehouseList);

        WarehouseSpinnerAdapter adapter = new WarehouseSpinnerAdapter(this, android.R.layout.simple_spinner_item, allWarehouses, warehouseIcon);
        spinnerWarehouses.setAdapter(adapter);

        spinnerWarehouses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    adjustCameraView();
                } else {
                    focusOnWarehouse(warehouseList.get(position - 1));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(5);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        isMapReady = true;
        addItemsAndWarehousesToMap();
    }

    private void addItemsAndWarehousesToMap() {
        if (!isMapReady || !isDataLoaded) return;

        for (Item item : itemList) {
            for (ItemWarehouse itemWarehouse : item.getItemWarehouses()) {
                LatLng itemLatLng = itemWarehouse.getLocation().toLatLng();
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(itemLatLng)
                        .title(item.getName())
                        .snippet("Quantity: " + itemWarehouse.getQuantity());

                if (itemWarehouse.getQuantity() == 0) {
                    markerOptions.icon(getBitmapDescriptor(outOfStockIcon));
                } else if (isNewItem(item)) {
                    markerOptions.icon(getBitmapDescriptor(R.drawable.ic_newbox));
                } else if (isOldItem(item)) {
                    markerOptions.icon(getBitmapDescriptor(R.drawable.ic_oldbox));
                } else {
                    markerOptions.icon(getBitmapDescriptor(R.drawable.ic_box));
                }
                map.addMarker(markerOptions);
                boundsBuilder.include(itemLatLng);
            }
        }

        for (Warehouse warehouse : warehouseList) {
            PolygonOptions polygonOptions = new PolygonOptions();
            List<LatLng> sortedPoints = sortPoints(warehouse.getPoints());
            for (LatLng point : sortedPoints) {
                polygonOptions.add(point);
                boundsBuilder.include(point);
            }
            map.addPolygon(polygonOptions);

            LatLng avgPoint = avgPoint(sortedPoints);
            map.addMarker(new MarkerOptions()
                    .position(avgPoint)
                    .title("Warehouse: " + warehouse.getName())
                    .icon(getBitmapDescriptor(R.drawable.ic_warehousemap)));
        }

        adjustCameraView();
    }

    private void focusOnWarehouse(Warehouse warehouse) {
        if (map != null) {
            List<LatLng> points = warehouse.getPoints();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }

    private boolean isNewItem(Item item) {
        long today = new Date().getTime();
        long itemDate = item.getLastModified().getTime();
        return (today - itemDate) < 86400000; // 24 hours in milliseconds
    }

    private boolean isOldItem(Item item) {
        long today = new Date().getTime();
        long itemDate = item.getLastModified().getTime();
        return (today - itemDate) > 2592000000L; // 30 days in milliseconds
    }

    private LatLng avgPoint(List<LatLng> sortedPoints) {
        double lat = 0;
        double lng = 0;
        for (LatLng point : sortedPoints) {
            lat += point.latitude;
            lng += point.longitude;
        }
        return new LatLng(lat / sortedPoints.size(), lng / sortedPoints.size());
    }

    public List<LatLng> sortPoints(List<LatLng> points) {
        if (points.size() != 4) {
            throw new IllegalArgumentException("There must be exactly 4 points.");
        }

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

        points.remove(bottomLeft);

        points.sort(new Comparator<LatLng>() {
            @Override
            public int compare(LatLng p1, LatLng p2) {
                double angle1 = Math.atan2(p1.latitude - bottomLeft.latitude, p1.longitude - bottomLeft.longitude);
                double angle2 = Math.atan2(p2.latitude - bottomLeft.latitude, p2.longitude - bottomLeft.longitude);
                return Double.compare(angle1, angle2);
            }
        });

        points.add(0, bottomLeft);

        return points;
    }

    private void adjustCameraView() {
        if (map != null && !itemList.isEmpty() && !warehouseList.isEmpty()) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, id);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
