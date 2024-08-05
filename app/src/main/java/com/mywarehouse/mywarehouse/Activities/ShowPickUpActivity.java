package com.mywarehouse.mywarehouse.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.mywarehouse.mywarehouse.Adapters.ShowPickUpAdapter;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseShowPickUp;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImagesAndLocations;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.CustomNestedScrollView;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ShowPickUpActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Order order;
    private RecyclerView recyclerViewPickupItems;
    private ShowPickUpAdapter showPickUpAdapter;
    private List<PickupItemWithImagesAndLocations> pickupItemWithImagesAndLocationsList;
    private GoogleMap map;
    private List<Marker> selectedMarkers;
    private BottomNavigationView bottomNavigationView;
    private MaterialButton acceptButton, denyButton;
    private CustomNestedScrollView customNestedScrollView;
    private HashMap<LatLng, PickupItem> locationItemMap;
    private HashMap<Marker, Boolean> markerSelectedMap;
    private HashMap<PickupItem, LatLng> pickupItemLocationMap;
    private boolean isCollectModeActive = false;
    private PickupItem currentPickupItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_pick_up);

        if (getIntent() != null && getIntent().hasExtra("order")) {
            order = getIntent().getParcelableExtra("order");
        }

        recyclerViewPickupItems = findViewById(R.id.recycler_view_pickup_items);
        recyclerViewPickupItems.setLayoutManager(new LinearLayoutManager(this));
        pickupItemWithImagesAndLocationsList = new ArrayList<>();
        selectedMarkers = new ArrayList<>();
        locationItemMap = new HashMap<>();
        markerSelectedMap = new HashMap<>();
        pickupItemLocationMap = new HashMap<>();

        showPickUpAdapter = new ShowPickUpAdapter(this, pickupItemWithImagesAndLocationsList, new ShowPickUpAdapter.OnItemClickListener() {
            @Override
            public void onMapClick(int position) {
                PickupItemWithImagesAndLocations item = pickupItemWithImagesAndLocationsList.get(position);
                currentPickupItem = item.getPickupItemWithImages().getPickupItem();
                updateSelectedMarkers(item.getLocations(), item.getPickupItemWithImages().getPickupItem());
            }

            @Override
            public void onCollectClick(int position) {
                if (currentPickupItem == null) {
                    Toast.makeText(ShowPickUpActivity.this, "Please select items on the map before collecting.", Toast.LENGTH_SHORT).show();
                    return;
                }

                PickupItemWithImagesAndLocations item = pickupItemWithImagesAndLocationsList.get(position);
                currentPickupItem = item.getPickupItemWithImages().getPickupItem();

                // Check if the item has multiple locations
                if (itemHasMultipleLocations(item.getPickupItemWithImages().getPickupItem())) {
                    promptUserToSelectMarker(item.getPickupItemWithImages().getPickupItem());
                } else {
                    // Use the position of the selected marker instead of the iterator's next key
                    if (!selectedMarkers.isEmpty()) {
                        LatLng selectedLocation = selectedMarkers.get(0).getPosition();
                        collectItem(item.getPickupItemWithImages().getPickupItem(), position, selectedLocation);
                    } else {
                        Toast.makeText(ShowPickUpActivity.this, "No marker selected.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        recyclerViewPickupItems.setAdapter(showPickUpAdapter);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigationView();

        acceptButton = findViewById(R.id.accept_button);
        denyButton = findViewById(R.id.deny_button);

        customNestedScrollView = findViewById(R.id.custom_nested_scroll_view);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        acceptButton.setOnClickListener(v -> handleAcceptButtonClick());

        denyButton.setOnClickListener(v -> handleDenyButtonClick());

        fetchPickupItemsWithImagesAndLocations();
    }

    private void fetchPickupItemsWithImagesAndLocations() {
        FirebaseShowPickUp.fetchPickupItemsWithImagesAndLocations(order.getPickupItems(), order.getSelectedWarehouse(), pickupItemsWithImagesAndLocations -> {
            pickupItemWithImagesAndLocationsList.clear();
            pickupItemWithImagesAndLocationsList.addAll(pickupItemsWithImagesAndLocations);
            showPickUpAdapter.notifyDataSetChanged();
            addMarkersToMap();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(17);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        }
        map.setOnMapClickListener(this::handleMapClick);
        map.setOnMarkerClickListener(this::handleMarkerClick);
        map.setOnCameraMoveStartedListener(reason -> customNestedScrollView.setScrollingEnabled(false));
        map.setOnCameraIdleListener(() -> customNestedScrollView.setScrollingEnabled(true));
        fetchWarehouseLocation(order.getSelectedWarehouse());
    }

    private void fetchWarehouseLocation(String warehouseName) {
        FirebaseShowPickUp.fetchWarehouse(warehouseName, warehouse -> {
            focusOnWarehouse(warehouse);
            addMarkersToMap();
        });
    }

    private void focusOnWarehouse(Warehouse warehouse) {
        if (map != null) {
            List<LatLng> points = sortPoints(warehouse.getPoints());
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            PolygonOptions polygonOptions = new PolygonOptions();
            for (LatLng point : points) {
                polygonOptions.add(point);
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            map.addPolygon(polygonOptions);
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            // Add warehouse marker in the center
            LatLng avgPoint = avgPoint(points);
            map.addMarker(new MarkerOptions()
                    .position(avgPoint)
                    .title("Warehouse: " + warehouse.getName())
                    .icon(getBitmapDescriptor(R.drawable.ic_warehousemap)));
        }
    }

    private void addMarkersToMap() {
        if (map != null) {
            for (PickupItemWithImagesAndLocations item : pickupItemWithImagesAndLocationsList) {
                for (ItemWarehouse itemWarehouse : item.getLocations()) {
                    LatLng location = itemWarehouse.getLocation().toLatLng();
                    Marker marker = map.addMarker(new MarkerOptions()
                            .position(location)
                            .title(item.getPickupItemWithImages().getPickupItem().getName())
                            .icon(getBitmapDescriptor(R.drawable.ic_box)));
                    locationItemMap.put(location, item.getPickupItemWithImages().getPickupItem());
                    markerSelectedMap.put(marker, false);
                }
            }
        }
    }

    private void updateSelectedMarkers(List<ItemWarehouse> locations, PickupItem pickupItem) {
        // Reset previous selected markers
        for (Marker marker : markerSelectedMap.keySet()) {
            if (markerSelectedMap.getOrDefault(marker, false)) {
                marker.setIcon(getBitmapDescriptor(R.drawable.ic_box));
                markerSelectedMap.put(marker, false);
            }
        }

        // Add new selected markers
        for (ItemWarehouse itemWarehouse : locations) {
            LatLng location = itemWarehouse.getLocation().toLatLng();
            for (Marker marker : markerSelectedMap.keySet()) {
                if (marker.getPosition().equals(location)) {
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_boxgreen));
                    markerSelectedMap.put(marker, true);
                    selectedMarkers.add(marker);
                }
            }
        }

        // Adjust the zoom level to fit all selected markers
        if (!selectedMarkers.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : selectedMarkers) {
                builder.include(marker.getPosition());
            }
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100)); // Padding of 50 pixels for a closer zoom
        }
    }

    private void promptUserToSelectMarker(PickupItem pickupItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_marker, null);

        builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> isCollectModeActive = true)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.white));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }


    private void collectItem(PickupItem pickupItem, int position, LatLng selectedLocation) {
        pickupItemWithImagesAndLocationsList.remove(position);
        showPickUpAdapter.notifyDataSetChanged();
        pickupItemLocationMap.put(pickupItem, selectedLocation);
        Toast.makeText(this, "Item collected", Toast.LENGTH_SHORT).show();
        resetSelectedMarkers();
    }

    private void resetSelectedMarkers() {
        for (Marker marker : selectedMarkers) {
            marker.setIcon(getBitmapDescriptor(R.drawable.ic_box));
            markerSelectedMap.put(marker, false);
        }
        selectedMarkers.clear();
    }

    private boolean itemHasMultipleLocations(PickupItem pickupItem) {
        List<LatLng> locations = new ArrayList<>();
        for (LatLng location : locationItemMap.keySet()) {
            if (locationItemMap.get(location).equals(pickupItem)) {
                locations.add(location);
            }
        }
        return locations.size() > 1;
    }

    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, id);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void setupBottomNavigationView() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }

    private void handleMapClick(LatLng latLng) {
        // No need to handle map clicks in this implementation
    }

    private boolean handleMarkerClick(Marker marker) {
        if (isCollectModeActive && markerSelectedMap.getOrDefault(marker, false)) {
            marker.setIcon(getBitmapDescriptor(R.drawable.ic_box));
            markerSelectedMap.put(marker, false);
            selectedMarkers.remove(marker);
            LatLng selectedLocation = marker.getPosition();
            pickupItemLocationMap.put(currentPickupItem, selectedLocation);

            int position = findPickupItemWithImagesIndex(currentPickupItem);
            if (position != -1) {
                collectItem(currentPickupItem, position, selectedLocation);
            }

            isCollectModeActive = false;
            return true;
        }
        return false;
    }

    private int findPickupItemWithImagesIndex(PickupItem pickupItem) {
        for (int i = 0; i < pickupItemWithImagesAndLocationsList.size(); i++) {
            if (pickupItemWithImagesAndLocationsList.get(i).getPickupItemWithImages().getPickupItem().equals(pickupItem)) {
                return i;
            }
        }
        return -1;
    }

    private LatLng avgPoint(List<LatLng> points) {
        double lat = 0;
        double lng = 0;
        for (LatLng point : points) {
            lat += point.latitude;
            lng += point.longitude;
        }
        return new LatLng(lat / points.size(), lng / points.size());
    }

    private List<LatLng> sortPoints(List<LatLng> points) {
        if (points.size() != 4) {
            throw new IllegalArgumentException("There must be exactly 4 points.");
        }

        LatLng bottomLeft = Collections.min(points, (p1, p2) -> {
            if (p1.latitude != p2.latitude) {
                return Double.compare(p1.latitude, p2.latitude);
            } else {
                return Double.compare(p1.longitude, p2.longitude);
            }
        });

        points.remove(bottomLeft);

        points.sort((p1, p2) -> {
            double angle1 = Math.atan2(p1.latitude - bottomLeft.latitude, p1.longitude - bottomLeft.longitude);
            double angle2 = Math.atan2(p2.latitude - bottomLeft.latitude, p2.longitude - bottomLeft.longitude);
            return Double.compare(angle1, angle2);
        });

        points.add(0, bottomLeft);

        return points;
    }

    private void handleAcceptButtonClick() {
        if (pickupItemWithImagesAndLocationsList.isEmpty()) {
            order.setStatus(OrderType.PICKED_UP);
            order.setCollectedBy(MyUser.getInstance().getDocumentId());
            FirebaseShowPickUp.updateOrder(order, new FirebaseShowPickUp.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    FirebaseShowPickUp.removeUserPickup(MyUser.getInstance().getDocumentId(), order.getOrderId(), new FirebaseShowPickUp.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            for (PickupItem pickupItem : pickupItemLocationMap.keySet()) {
                                updateItemQuantity(pickupItem, pickupItemLocationMap.get(pickupItem));
                            }
                            Toast.makeText(ShowPickUpActivity.this, "Collected successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ShowPickUpActivity.this, OrdersActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // Handle failure to remove user pickup
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    // Handle failure to update order status
                }
            });
        } else {
            Toast.makeText(this, "Please collect all items before accepting.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDenyButtonClick() {
        order.setStatus(OrderType.REGISTERED);
        FirebaseShowPickUp.updateOrder(order, new FirebaseShowPickUp.FirestoreCallback() {
            @Override
            public void onSuccess() {
                FirebaseShowPickUp.removeUserPickup(MyUser.getInstance().getDocumentId(), order.getOrderId(), new FirebaseShowPickUp.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        Intent intent = new Intent(ShowPickUpActivity.this, OrdersActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Handle failure to remove user pickup
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Handle failure to update order status
            }
        });
    }

    private void updateItemQuantity(PickupItem pickupItem, LatLng location) {
        FirebaseShowPickUp.fetchItem(pickupItem.getBarcode() + "_" + pickupItem.getName(), item -> {
            for (ItemWarehouse itemWarehouse : item.getItemWarehouses()) {
                if (itemWarehouse.getWarehouseName().equals(order.getSelectedWarehouse())
                        && itemWarehouse.getLocation().toLatLng().equals(location)) {
                    itemWarehouse.setQuantity(itemWarehouse.getQuantity() - pickupItem.getQuantity());
                    item.setTotalQuantity(item.getTotalQuantity() - pickupItem.getQuantity());
                    item.setRequestedAmount(item.getRequestedAmount() - pickupItem.getQuantity());
                    FirebaseShowPickUp.updateItem(item, new FirebaseShowPickUp.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            // Item quantity updated
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // Failed to update item quantity
                        }
                    });
                    break;
                }
            }
        });
    }

    private interface LocationCallback {
        void onLocationFetched(List<ItemWarehouse> locations);
    }
}
