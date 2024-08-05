package com.mywarehouse.mywarehouse.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.mywarehouse.mywarehouse.Adapters.ImageAdapter;
import com.mywarehouse.mywarehouse.Adapters.WarehouseAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseUpdateItemBundled;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.CustomNestedScrollView;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
public class UpdateItemBundledActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextInputEditText  inputBarcode, inputName, inputSupplier, inputDescription;
    private MaterialButton   buttonUpdateItem;
    private AppCompatImageButton buttonAttachImages, buttonCaptureImage;
    private List<ItemWarehouse> itemWarehouseList = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private CustomNestedScrollView customNestedScrollView;
    private RecyclerView recyclerImages,recyclerWarehouses;;
    private WarehouseAdapter warehouseAdapter;
    private BottomNavigationView bottomNavigationView;
    private Spinner spinnerWarehouse;
    private Intent intent = null;
    private ImageAdapter imageAdapter;
    private GoogleMap map;
    private String currentPhotoPath;
    private ArrayList<String> addedImages;
    private int imagesToUploadCount = 0;
    private int imagesUploadedCount = 0;
    private boolean doneSuccessfully = false;
    private List<Warehouse> warehouseList = new ArrayList<>();
    private Warehouse selectedWarehouse;
    private Item currentItem;
    private String documentId;
    private boolean isItemFound = false;
    private Polygon lastpolygon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_item_bundled);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        findViews();
        initViews();
        loadWarehouses();
        enableFields(false);
        if (getIntent().hasExtra("item")) {
            currentItem = getIntent().getParcelableExtra("item");
            // Assuming you pass documentId as well
            if (currentItem != null) {
                getDocumentId(currentItem);
            }
        }

    }
    private void getDocumentId(Item item) {
        FirebaseUpdateItemBundled.getDocumentId(item, newItem -> {
            if (newItem != null) {
                currentItem = newItem;
                documentId= item.getBarcode() + "_" + item.getName();
                isItemFound = true;
                populateFields(currentItem);
                enableFields(true);
            } else {
                Toast.makeText(UpdateItemBundledActivity.this, "Item not found. Please check the barcode and name.", Toast.LENGTH_SHORT).show();
                isItemFound = false;
                Intent intent = new Intent(UpdateItemBundledActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    private void findViews() {
        inputBarcode = findViewById(R.id.input_barcode);
        inputName = findViewById(R.id.input_name);
        inputSupplier = findViewById(R.id.input_supplier);
        inputDescription = findViewById(R.id.input_description);
        buttonUpdateItem = findViewById(R.id.button_update_item);
        buttonAttachImages = findViewById(R.id.button_attach_images);
        buttonCaptureImage = findViewById(R.id.button_capture_image);
        recyclerImages = findViewById(R.id.recycler_images);
        recyclerWarehouses = findViewById(R.id.recycler_warehouses);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        spinnerWarehouse = findViewById(R.id.spinner_warehouse);
        customNestedScrollView = findViewById(R.id.custom_nested_scroll_view);

    }

    private void initViews() {
        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this);
        recyclerImages.setAdapter(imageAdapter);
        addedImages = new ArrayList<>();

        buttonUpdateItem.setOnClickListener(v -> checkAndUpdateItem());


        buttonAttachImages.setOnClickListener(v -> openFileChooser());

        buttonCaptureImage.setOnClickListener(v -> captureImage());

        spinnerWarehouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWarehouse = warehouseList.get(position);
                showWarehouseOnMap(selectedWarehouse);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedWarehouse = null;
            }
        });

        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);

        bottomNavigationView.setSelectedItemId(R.id.navigation_inventory);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (imagesToUploadCount == imagesUploadedCount) {
                int id = item.getItemId();
                if (id == R.id.navigation_inventory) {
                    intent = new Intent(this, InventoryActivity.class);
                } else if (id == R.id.navigation_account) {
                    intent = new Intent(this, AccountActivity.class);
                } else if (id == R.id.navigation_reports) {
                    intent = new Intent(this, ReportsActivity.class);
                } else if (id == R.id.navigation_orders) {
                    intent = new Intent(this, OrdersActivity.class);
                } else if (id == R.id.navigation_home) {
                    intent = new Intent(this, HomeActivity.class);
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                }
                return true;
            } else {
                Toast.makeText(this, "Please wait for all images to be uploaded", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        recyclerWarehouses.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        warehouseAdapter = new WarehouseAdapter(itemWarehouseList);
        recyclerWarehouses.setAdapter(warehouseAdapter);
    }



    private void populateFields(Item item) {
        inputBarcode.setText(item.getBarcode());
        inputName.setText(item.getName());
        inputDescription.setText(item.getDescription());
        inputSupplier.setText(item.getSupplier());
        enableFieldsMap(true);
        enableFields(true);

        // Clear the map and add the marker for the item's location
        itemWarehouseList.addAll(item.getItemWarehouses());
        warehouseAdapter.notifyDataSetChanged();

        for (String url : item.getImageUrls()) {
            imageAdapter.addDefaultImage(UUID.randomUUID().toString(), Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.loading_gif)); // Add default image with unique ID
            Uri uri = Uri.parse(url);
            imageAdapter.updateImageUri(imageAdapter.getLastItemId(), uri, uri.toString());
        }
        if(map!=null)
        {
            if(markers.isEmpty()) {
                for (ItemWarehouse itemWarehouse : itemWarehouseList) {
                    Marker marker = map.addMarker(new MarkerOptions().position(itemWarehouse.getLocation().toLatLng()));
                    markers.add(marker);
                }
            }
        }
        if(!warehouseList.isEmpty() || !itemWarehouseList.isEmpty()) {
            for (int x = 0; x < warehouseList.size(); x++) {
                if (warehouseList.get(x).getName().equals(itemWarehouseList.get(0).getWarehouseName()) ) {
                    spinnerWarehouse.setSelection(x);
                    break;
                }
            }
        }
    }




    private void enableFields(boolean enabled) {
        inputDescription.setEnabled(enabled);
        inputSupplier.setEnabled(enabled);
        buttonUpdateItem.setEnabled(enabled);
        buttonAttachImages.setEnabled(enabled);
        buttonCaptureImage.setEnabled(enabled);
        buttonUpdateItem.setEnabled(enabled);
        inputBarcode.setEnabled(!enabled);
        inputName.setEnabled(!enabled);
    }

    private void enableFieldsMap(boolean enabled) {
        if (map != null) {
            map.getUiSettings().setAllGesturesEnabled(enabled);
            map.getUiSettings().setCompassEnabled(false);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(15);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        }
        if(isItemFound)
        {
            if(markers.isEmpty()) {
                for (ItemWarehouse itemWarehouse : itemWarehouseList) {
                    Marker marker = map.addMarker(new MarkerOptions().position(itemWarehouse.getLocation().toLatLng()));
                    markers.add(marker);
                }
            }
            if(!warehouseList.isEmpty() || !itemWarehouseList.isEmpty()) {
                for (int x = 0; x < warehouseList.size(); x++) {
                    if (warehouseList.get(x).getName().equals(itemWarehouseList.get(0).getWarehouseName()) ) {
                        spinnerWarehouse.setSelection(x);
                        break;
                    }
                }
            }
        }
        map.setOnMapClickListener(this::handleMapClick);
        map.setOnMarkerClickListener(this::removeMarkerAndItem);
        map.setOnCameraMoveStartedListener(reason -> customNestedScrollView.setScrollingEnabled(false));
        map.setOnCameraIdleListener(() -> customNestedScrollView.setScrollingEnabled(true));
    }

    private void handleMapClick(LatLng latLng) {
        if (!isLocationInsideWarehouse(latLng,warehouseList.get(spinnerWarehouse.getSelectedItemPosition()).getPoints())) {
            Toast.makeText(this, "Location must be inside a warehouse", Toast.LENGTH_SHORT).show();
            return;
        }

        Marker marker = map.addMarker(new MarkerOptions().position(latLng));
        markers.add(marker);

        String warehouseName = warehouseList.get(spinnerWarehouse.getSelectedItemPosition()).getName();
        ItemWarehouse itemWarehouse = new ItemWarehouse(warehouseName, latLng, 0);
        itemWarehouseList.add(itemWarehouse);
        warehouseAdapter.notifyItemInserted(itemWarehouseList.size() - 1);
    }

    private boolean removeMarkerAndItem(Marker marker) {
        int index = markers.indexOf(marker);
        if (index != -1) {
            marker.remove();
            markers.remove(index);
            itemWarehouseList.remove(index);
            warehouseAdapter.notifyItemRemoved(index);
        }
        return true;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        selectImageFromGallery.launch(intent);
    }

    private final ActivityResultLauncher<Intent> selectImageFromGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri imageUri = result.getData().getData();
                    imageAdapter.addDefaultImage(UUID.randomUUID().toString(), Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.loading_gif)); // Add default image with unique ID
                    uploadImageToFirebase(imageUri, imageAdapter.getLastItemId());
                }
            }
    );

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.mywarehouse.mywarehouse.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                captureImageFromCamera.launch(takePictureIntent);
            }
        }
    }

    private final ActivityResultLauncher<Intent> captureImageFromCamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File f = new File(currentPhotoPath);
                    Uri imageUri = Uri.fromFile(f);
                    imageAdapter.addDefaultImage(UUID.randomUUID().toString(), Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.loading_gif)); // Add default image with unique ID
                    bottomNavigationView.setVisibility(View.INVISIBLE);
                    imagesToUploadCount++;
                    uploadImageToFirebase(imageUri, imageAdapter.getLastItemId());
                }
            }
    );

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadImageToFirebase(Uri imageUri, String imageId) {
        if (imageUri != null) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("items").child(imageUri.getLastPathSegment());
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        imagesUploadedCount++;
                        addedImages.add(uri.toString());
                        imageAdapter.updateImageUri(imageId, uri, uri.toString());
                        if (imagesToUploadCount == imagesUploadedCount) {
                            bottomNavigationView.setVisibility(View.VISIBLE);
                        }
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        imagesToUploadCount--;
                    });
        }
    }

    private void checkAndUpdateItem() {
        if (imagesToUploadCount != imagesUploadedCount) {
            Toast.makeText(this, "Images still uploading please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        String description = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
        String supplier = inputSupplier.getText() != null ? inputSupplier.getText().toString().trim() : "";

        if (description.isEmpty() ||  supplier.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (ItemWarehouse itemWarehouse : itemWarehouseList) {
            if (itemWarehouse.getQuantity() <= 0) {
                Toast.makeText(this, "Each quantity must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        saveItem(currentItem.getBarcode(), currentItem.getName(),description,supplier);
    }

    private void saveItem(String barcode, String name, String description, String supplier) {
        int totalQuantity = calculateTotalQuantity();

        if (totalQuantity < 0) {
            Toast.makeText(this, "Total quantity must be a positive number.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (totalQuantity == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_custom, null);

            builder.setView(dialogView)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Date currentDate = new Date();
                        Item updatedItem = new Item(barcode, name, description, totalQuantity, imageAdapter.getImageUrls(), true, supplier, currentDate, itemWarehouseList, currentItem.getRequestedAmount());

                        FirebaseUpdateItemBundled.saveLog(updatedItem, currentItem, MyUser.getInstance().getName(), new FirebaseUpdateItemBundled.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                FirebaseUpdateItemBundled.saveItem(documentId, updatedItem, new FirebaseUpdateItemBundled.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(UpdateItemBundledActivity.this, "Item saved", Toast.LENGTH_SHORT).show();
                                        FirebaseUpdateItemBundled.saveOutOfStockLog(updatedItem.getName(), updatedItem.getBarcode(), currentDate, MyUser.getInstance().getName(), new FirebaseUpdateItemBundled.FirestoreCallback() {
                                            @Override
                                            public void onSuccess() {
                                                // Log saved successfully
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Toast.makeText(UpdateItemBundledActivity.this, "Failed to save log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        doneSuccessfully = true;
                                        addedImages.clear();
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(UpdateItemBundledActivity.this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(UpdateItemBundledActivity.this, "Failed to save log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.white));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            Date currentDate = new Date();
            Item updatedItem = new Item(barcode, name, description, totalQuantity, imageAdapter.getImageUrls(), true, supplier, currentDate, itemWarehouseList, currentItem.getRequestedAmount());

            FirebaseUpdateItemBundled.saveLog(updatedItem, currentItem, MyUser.getInstance().getName(), new FirebaseUpdateItemBundled.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    FirebaseUpdateItemBundled.saveItem(documentId, updatedItem, new FirebaseUpdateItemBundled.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(UpdateItemBundledActivity.this, "Item saved", Toast.LENGTH_SHORT).show();

                            doneSuccessfully = true;
                            addedImages.clear();
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(UpdateItemBundledActivity.this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(UpdateItemBundledActivity.this, "Failed to save log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private int calculateTotalQuantity() {
        int totalQuantity = 0;
        for (ItemWarehouse itemWarehouse : itemWarehouseList) {
            totalQuantity += itemWarehouse.getQuantity();
        }
        return totalQuantity;
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

        // Ensure a.y <= b.y
        if (ay > by) {
            double tempX = ax, tempY = ay;
            ax = bx;
            ay = by;
            bx = tempX;
            by = tempY;
        }

        // Check if the point is outside the vertical range of the segment
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


    private void showWarehouseOnMap(Warehouse warehouse) {
        if (map == null || warehouse == null || warehouse.getPoints() == null || warehouse.getPoints().size() < 3) {
            return;
        }
        PolygonOptions polygonOptions = new PolygonOptions();
        List<LatLng> sortedPoints = sortPoints(warehouse.getPoints());
        for (LatLng point : sortedPoints) {
            polygonOptions.add(point);
        }
        if(lastpolygon!=null) {
            lastpolygon.remove();
        }
        lastpolygon= map.addPolygon(polygonOptions);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(avgPoint(sortedPoints), 19));
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
    private void loadWarehouses() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUpdateItemBundled.fetchWarehouses(db, warehouseList -> {
            if (warehouseList != null) {
                this.warehouseList.clear();
                this.warehouseList.addAll(warehouseList);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getWarehouseNames());
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerWarehouse.setAdapter(adapter);
            } else {
                Toast.makeText(this, "Failed to load warehouses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> getWarehouseNames() {
        List<String> names = new ArrayList<>();
        for (Warehouse warehouse : warehouseList) {
            names.add(warehouse.getName());
        }
        return names;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteImageFromFirebase(ArrayList<String> urls) {
        if (urls == null) return;
        for (String url : urls) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            imageRef.delete().addOnSuccessListener(aVoid -> {
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to reset images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }
        urls.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            if (imagesUploadedCount > 0 && !doneSuccessfully) {
                if (!addedImages.isEmpty()) {
                    deleteImageFromFirebase(addedImages);
                    Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
