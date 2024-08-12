package com.mywarehouse.mywarehouse.Activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.mywarehouse.mywarehouse.Adapters.AcceptTransactionAdapter;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseAcceptTransaction;
import com.mywarehouse.mywarehouse.Interfaces.DataLoadCallback;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.Models.WarehouseQuantityRange;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.CustomNestedScrollView;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcceptTransactionActivity extends AppCompatActivity implements DataLoadCallback, OnMapReadyCallback {

    private RecyclerView recyclerViewPickupItems;
    private MaterialButton acceptButton, denyButton;
    private TransactionRequest transactionRequest;
    private AcceptTransactionAdapter adapter;
    private CustomNestedScrollView customNestedScrollView;
    private GoogleMap map;
    public Warehouse currentWarehouse;
    private HashMap<PickupItem, LatLng> pickupItemLocationMap = new HashMap<>();
    public Map<PickupItem, Boolean> editingModeMap = new HashMap<>();
    public Map<PickupItem, Marker> itemMarkerMap = new HashMap<>();
    private HashMap<Marker, PickupItem> markerItemMap = new HashMap<>();
    private boolean endSucessfully=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_transaction);

        recyclerViewPickupItems = findViewById(R.id.recycler_view_pickup_items);
        acceptButton = findViewById(R.id.accept_button);
        denyButton = findViewById(R.id.deny_button);
        customNestedScrollView = findViewById(R.id.custom_nested_scroll_view);
        transactionRequest = getIntent().getParcelableExtra("transactionRequest");

        // Check if the transaction is already being updated
        FirebaseAcceptTransaction.fetchTransactionRequest(transactionRequest.getRequestId(), new FirebaseAcceptTransaction.TransactionRequestCallback() {
            @Override
            public void onCallback(TransactionRequest request) {
                if (request != null) {

                        // Set onUpdate to true and continue with initialization
                        FirebaseAcceptTransaction.updateTransactionRequestOnUpdateStatus(transactionRequest.getRequestId(), true, new FirebaseAcceptTransaction.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                initializeActivity();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(AcceptTransactionActivity.this, "Failed to lock the request. Try again later.", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        });
                } else {
                    Toast.makeText(AcceptTransactionActivity.this, "Failed to fetch the request status.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AcceptTransactionActivity.this, "Failed to fetch the request status.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void initializeActivity() {
        setupRecyclerView();
        setupButtons();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(AcceptTransactionActivity.this);
        }
    }

    private void setupRecyclerView() {
        recyclerViewPickupItems.setLayoutManager(new LinearLayoutManager(this));
        List<PickupItem> pickupItemList = transactionRequest.getRequestedItemsToMove();
        adapter = new AcceptTransactionAdapter(this, pickupItemList, transactionRequest, this);
        recyclerViewPickupItems.setAdapter(adapter);
    }

    private void setupButtons() {
        acceptButton.setOnClickListener(v -> handleAcceptTransaction());

        denyButton.setOnClickListener(v -> finish());
    }

    private void unlockItems() {
        if(adapter.getPickupItemList()==null)return;
        for (PickupItem item : adapter.getPickupItemList()) {
            String documentId = item.getBarcode() + "_" + item.getName();
            FirebaseAcceptTransaction.updateItemOnUpdateStatus(documentId, false, new FirebaseAcceptTransaction.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    // Successfully unlocked item
                }

                @Override
                public void onFailure(Exception e) {
                    // Handle the failure case
                }
            });
        }
    }

    @Override
    public void onDataLoaded() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
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

        FirebaseAcceptTransaction.fetchWarehouse(transactionRequest.getWarehouse(), warehouse -> {
            currentWarehouse = warehouse;
            focusOnWarehouse();
        });
    }

    public void focusOnWarehouse() {
        if (map != null && currentWarehouse != null) {
            List<LatLng> points = currentWarehouse.getPoints();
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
                    .title("Warehouse: " + currentWarehouse.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_warehousemap)));

        }
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



    private void handleMapClick(LatLng latLng) {
        PickupItem selectedItem = adapter.getCurrentSelectedPickupItem();
        if (selectedItem != null && editingModeMap.getOrDefault(selectedItem, false)) {
            if (!isLocationInsideWarehouse(latLng, currentWarehouse.getPoints())) {
                Toast.makeText(this, "Location is outside the warehouse boundaries.", Toast.LENGTH_SHORT).show();
                return;
            }

            Marker existingMarker = itemMarkerMap.get(selectedItem);
            if (existingMarker != null) {
                existingMarker.remove();
                markerItemMap.remove(existingMarker);
            }

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(selectedItem.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_boxgreen)));

            pickupItemLocationMap.put(selectedItem, latLng);
            itemMarkerMap.put(selectedItem, marker);
            markerItemMap.put(marker, selectedItem);
            Toast.makeText(this, "Location set for " + selectedItem.getName(), Toast.LENGTH_SHORT).show();
            editingModeMap.put(selectedItem, false);
        }
    }

    private boolean isLocationInsideWarehouse(LatLng location, List<LatLng> points) {
        int crossings = 0;
        int pointCount = points.size();
        for (int i = 0; i < pointCount; i++) {
            LatLng a = points.get(i);
            LatLng b = points.get((i + 1) % pointCount);
            if (rayCrossesSegment(location, a, b)) {
                crossings++;
            }
        }
        return (crossings % 2 == 1);
    }

    private boolean rayCrossesSegment(LatLng point, LatLng a, LatLng b) {
        double px = point.longitude;
        double py = point.latitude;
        double ax = a.longitude;
        double ay = a.latitude;
        double bx = b.longitude;
        double by = b.latitude;

        if (ay > by) {
            double tempX = ax, tempY = ay;
            ax = bx;
            ay = by;
            bx = tempX;
            by = tempY;
        }

        if (py == ay || py == by) {
            py += 0.00000001;
        }

        if (py < ay || py > by || px > Math.max(ax, bx)) {
            return false;
        }

        if (px < Math.min(ax, bx)) {
            return true;
        }

        double red = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.POSITIVE_INFINITY;
        double blue = (ax != px) ? ((py - ay) / (px - ax)) : Double.POSITIVE_INFINITY;

        return (blue >= red);
    }

    private boolean handleMarkerClick(Marker marker) {
        if (markerItemMap.containsKey(marker)) {
            PickupItem item = markerItemMap.get(marker);
            if (editingModeMap.getOrDefault(item, false)) {
                pickupItemLocationMap.remove(item);
                marker.remove();
                itemMarkerMap.remove(item);
                markerItemMap.remove(marker);
                Toast.makeText(this, "Marker removed for " + item.getName(), Toast.LENGTH_SHORT).show();
                editingModeMap.put(item, false);
                return true;
            } else {
                Toast.makeText(this, "Editing mode is not enabled for this item.", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    private void handleAcceptTransaction() {
        List<PickupItem> pickupItemList = adapter.getPickupItemList();
        boolean allItemsHaveLocations = true;
        StringBuilder missingLocations = new StringBuilder();

        for (PickupItem item : pickupItemList) {
            if (!pickupItemLocationMap.containsKey(item)) {
                allItemsHaveLocations = false;
                missingLocations.append(item.getName()).append(" ");
            }
        }

        if (!allItemsHaveLocations) {
            Toast.makeText(this, "Set new locations for the following items: " + missingLocations.toString(), Toast.LENGTH_LONG).show();
            return;
        }

        Map<PickupItem, WarehouseQuantityRange> selectedWarehouseQuantityRangeMap = adapter.getSelectedWarehouseQuantityRangeMap();

        for (PickupItem item : pickupItemList) {
            LatLng newLocation = pickupItemLocationMap.get(item);
            WarehouseQuantityRange selectedRange = selectedWarehouseQuantityRangeMap.get(item);
            ItemWarehouse sourceWarehouseItem = selectedRange.getWarehouseKey().getItemWarehouse();

            int selectedQuantity = selectedRange.getDesiredQuantity();

            if (sourceWarehouseItem != null) {
                int updatedSourceQuantity = sourceWarehouseItem.getQuantity() - selectedQuantity;
                if (updatedSourceQuantity < 0) {
                    Toast.makeText(this, "Not enough quantity in source warehouse for " + item.getName(), Toast.LENGTH_SHORT).show();
                    return;
                }
                sourceWarehouseItem.setQuantity(updatedSourceQuantity);
            }

            ItemWarehouse newWarehouseItem = new ItemWarehouse(transactionRequest.getWarehouse(), newLocation, selectedQuantity);
            FirebaseAcceptTransaction.fetchItem(item.getBarcode() + "_" + item.getName(), existingItem -> {
                if (existingItem != null) {
                    for (ItemWarehouse warehouseItem : existingItem.getItemWarehouses()) {
                        if (warehouseItem.equals(sourceWarehouseItem)) {
                            if(sourceWarehouseItem.getQuantity()==0)
                            {
                                existingItem.getItemWarehouses().remove(warehouseItem);
                            }
                            else warehouseItem.setQuantity(sourceWarehouseItem.getQuantity());
                            break;
                        }
                    }

                    existingItem.getItemWarehouses().add(newWarehouseItem);
                    existingItem.setLastModified(new Date());
                    FirebaseAcceptTransaction.updateItem(existingItem);
                }
            });
        }
        FirebaseAcceptTransaction.fetchOrder(transactionRequest.getOrderId(), new FirebaseAcceptTransaction.OrderCallback() {
            @Override
            public void onCallback(Order order) {
                if (order != null) {
                    order.setStatus(OrderType.IN_PROGRESS);
                    FirebaseAcceptTransaction.updateOrder(order, new FirebaseAcceptTransaction.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            FirebaseAcceptTransaction.deleteTransactionRequest(transactionRequest.getRequestId(), new FirebaseAcceptTransaction.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(AcceptTransactionActivity.this, "Transaction accepted and updated.", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    endSucessfully=true;
                                    finish();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    setResult(RESULT_CANCELED);
                                    finish();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!endSucessfully)unlockRequest();
    }

    private void unlockRequest() {
        unlockItems();
        FirebaseAcceptTransaction.updateTransactionRequestOnUpdateStatus(transactionRequest.getRequestId(), false, new FirebaseAcceptTransaction.FirestoreCallback() {
            @Override
            public void onSuccess() {
                setResult(RESULT_CANCELED);
                Toast.makeText(AcceptTransactionActivity.this, "Denied", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                setResult(RESULT_CANCELED);
                Toast.makeText(AcceptTransactionActivity.this, "Failed to release the lock.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
