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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ShowPickUpActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Order order;
    private RecyclerView recyclerViewPickupItems;
    private ShowPickUpAdapter showPickUpAdapter;
    private List<PickupItemWithImagesAndLocations> pickupItemWithImagesAndLocationsList;
    private GoogleMap map;
    private List<Marker> selectedMarkers;
    private MaterialButton acceptButton, denyButton;
    private CustomNestedScrollView customNestedScrollView;
    private HashMap<Marker, PickupItem> markerItemMap;
    private HashMap<Marker, Boolean> markerSelectedMap;
    private HashMap<Marker,Boolean> markerChoosenMap;
    private HashMap<PickupItem, Integer> pickupItemQuantityMap;
    private HashMap<Marker, ItemWarehouse> markerItemWarehouseHashMap;
    private HashMap<ItemWarehouse,Marker> itemWarehouseMarkerHashMap;
    private HashMap<ItemWarehouse, Integer> itemWarehouseMarkerQuantityMap;
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
        markerItemMap = new HashMap<>();
        markerSelectedMap = new HashMap<>();
        pickupItemQuantityMap = new HashMap<>();
        markerItemWarehouseHashMap = new HashMap<>();
        itemWarehouseMarkerQuantityMap=new HashMap<>();
        itemWarehouseMarkerHashMap=new HashMap<>();
        markerChoosenMap=new HashMap<>();

        showPickUpAdapter = new ShowPickUpAdapter(this, pickupItemWithImagesAndLocationsList, new ShowPickUpAdapter.OnItemClickListener() {
            @Override
            public void onMapClick(int position, List<ItemWarehouse> warehouses) {
                PickupItemWithImagesAndLocations item = pickupItemWithImagesAndLocationsList.get(position);
                currentPickupItem = item.getPickupItemWithImages().getPickupItem();
                updateSelectedMarkers(warehouses, currentPickupItem);
                promptUserToSelectMarker();
            }
        });
        recyclerViewPickupItems.setAdapter(showPickUpAdapter);


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
        FirebaseShowPickUp.fetchPickupItemsWithImagesAndLocations(pickupItemQuantityMap, order.getPickupItems(), order.getSelectedWarehouse(), pickupItemsWithImagesAndLocations -> {
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
            List<LatLng> points = warehouse.getPoints();
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
    private LatLng avgPoint(List<LatLng> points) {
        double latSum = 0;
        double lngSum = 0;

        for (LatLng point : points) {
            latSum += point.latitude;
            lngSum += point.longitude;
        }

        return new LatLng(latSum / points.size(), lngSum / points.size());
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
                    markerItemMap.put(marker, item.getPickupItemWithImages().getPickupItem());
                    markerItemWarehouseHashMap.put(marker, itemWarehouse);
                    itemWarehouseMarkerHashMap.put(itemWarehouse,marker);
                    markerSelectedMap.put(marker, false);
                }
            }
        }
    }

    private void updateSelectedMarkers(List<ItemWarehouse> locations, PickupItem pickupItem) {
        resetSelectedMarkers();

        List<ItemWarehouse> selectedWarehouses = new ArrayList<>();
        for (ItemWarehouse itemWarehouse : locations) {
                    Marker marker= itemWarehouseMarkerHashMap.get(itemWarehouse);
                    if(markerChoosenMap.getOrDefault(marker,false))
                    {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_redbox));
                    }
                    else marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_boxgreen));
                    markerSelectedMap.put(marker, true);
                    selectedMarkers.add(marker);

                    selectedWarehouses.add(itemWarehouse);
                }
            }


    private void promptUserToSelectMarker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_marker, null);

        builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> isCollectModeActive = true)
                .setNegativeButton("Cancel", (dialog, which) ->isCollectModeActive = false);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.white));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }


    private void resetSelectedMarkers() {
        for (Marker marker : selectedMarkers) {
            marker.setIcon(getBitmapDescriptor(R.drawable.ic_box));
            markerSelectedMap.put(marker, false);
        }
        selectedMarkers.clear();
    }

    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, id);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    private void handleMapClick(LatLng latLng) {
        // No need to handle map clicks in this implementation
    }

    private boolean handleMarkerClick(Marker marker) {

        if (isCollectModeActive) {
            if (markerSelectedMap.getOrDefault(marker, false)) {
                if (markerChoosenMap.getOrDefault(marker, false)) {
                    marker.setIcon(getBitmapDescriptor(R.drawable.ic_boxgreen));
                    ItemWarehouse selectedWarehouse = markerItemWarehouseHashMap.get(marker);
                    int quantity = itemWarehouseMarkerQuantityMap.get(selectedWarehouse);
                    pickupItemQuantityMap.put(currentPickupItem, pickupItemQuantityMap.get(currentPickupItem) + quantity);
                    markerChoosenMap.remove(marker);
                    return true;
                } else {

                    marker.setIcon(getBitmapDescriptor(R.drawable.ic_redbox));
                    ItemWarehouse selectedWarehouse = markerItemWarehouseHashMap.get(marker);
                    markerChoosenMap.put(marker, true);

                    int remainingQuantity = pickupItemQuantityMap.get(currentPickupItem);
                    if (selectedWarehouse.getQuantity() < remainingQuantity) {
                        itemWarehouseMarkerQuantityMap.put(selectedWarehouse, selectedWarehouse.getQuantity());
                        pickupItemQuantityMap.put(currentPickupItem, remainingQuantity - selectedWarehouse.getQuantity());
                        Toast.makeText(ShowPickUpActivity.this, "In order to complete the selection pick more points, remaining amount: "+ (remainingQuantity - selectedWarehouse.getQuantity()) , Toast.LENGTH_LONG).show();
                    } else {
                        itemWarehouseMarkerQuantityMap.put(selectedWarehouse, remainingQuantity);
                        pickupItemQuantityMap.put(currentPickupItem, 0);
                        isCollectModeActive = false;
                        Toast.makeText(ShowPickUpActivity.this, "Selection for pickup Item : "+currentPickupItem.getName() +" completed, you can edit it in anytime." , Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            }
            else {
                Toast.makeText(ShowPickUpActivity.this, "Choose markers in green or red", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        else {
            Toast.makeText(ShowPickUpActivity.this, "To choose markers press on the map mode", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void handleAcceptButtonClick() {
        if(isCollectModeActive)
        {
            Toast.makeText(ShowPickUpActivity.this, "Please finish selection mode first", Toast.LENGTH_LONG).show();
            return ;
        }
        for (int quantity:
             pickupItemQuantityMap.values()) {
            if(quantity!=0)
            {
                Toast.makeText(ShowPickUpActivity.this, "Make sure that you picked the right amount for each item", Toast.LENGTH_LONG).show();
                return ;
            }
        }
            order.setStatus(OrderType.PICKED_UP);
            order.setCollectedBy(MyUser.getInstance().getUser().getUserId());
            FirebaseShowPickUp.updateOrder(order, new FirebaseShowPickUp.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    FirebaseShowPickUp.removeUserPickup(MyUser.getInstance().getUser().getUserId(), order.getOrderId(), new FirebaseShowPickUp.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            for (PickupItem pickupItem : order.getPickupItems()) {
                                    updateItemQuantity(pickupItem);
                            }
                            Toast.makeText(ShowPickUpActivity.this, "Collected successfully", Toast.LENGTH_SHORT).show();
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

    private void handleDenyButtonClick() {
        order.setStatus(OrderType.REGISTERED);
        FirebaseShowPickUp.updateOrder(order, new FirebaseShowPickUp.FirestoreCallback() {
            @Override
            public void onSuccess() {
                FirebaseShowPickUp.removeUserPickup(MyUser.getInstance().getUser().getUserId(), order.getOrderId(), new FirebaseShowPickUp.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        Intent intent = new Intent(ShowPickUpActivity.this, PickupOrdersActivity.class);
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

    private void updateItemQuantity(PickupItem pickupItem) {
        FirebaseShowPickUp.fetchItem(pickupItem.getBarcode() + "_" + pickupItem.getName(), item -> {
            Iterator<ItemWarehouse> iterator = item.getItemWarehouses().iterator();

            while (iterator.hasNext()) {
                ItemWarehouse warehouse = iterator.next();
                int newQuantity = itemWarehouseMarkerQuantityMap.getOrDefault(warehouse, -1);

                if (newQuantity == -1) {
                    warehouse.setQuantity(warehouse.getQuantity());
                } else {
                    int updatedQuantity = warehouse.getQuantity() - newQuantity;
                    if (updatedQuantity <= 0) {
                        // Remove the warehouse from the list if the quantity reaches zero or below
                        iterator.remove();
                    } else {
                        warehouse.setQuantity(updatedQuantity);
                    }
                }
            }
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
        });
    }
}
