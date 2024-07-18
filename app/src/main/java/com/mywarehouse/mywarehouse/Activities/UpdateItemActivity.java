package com.mywarehouse.mywarehouse.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mywarehouse.mywarehouse.Adapters.ImageAdapter;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.CustomNestedScrollView;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class UpdateItemActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextInputEditText inputSupplier, inputBarcode, inputName, inputDescription, inputQuantity, inputLocation;
    private MaterialButton buttonReset, buttonSearch, buttonScanBarcode, buttonUpdateItem, buttonAttachImages, buttonCaptureImage;
    private CustomNestedScrollView customNestedScrollView;
    private RecyclerView recyclerImages;
    private BottomNavigationView bottomNavigationView;
    private Spinner spinnerWarehouse;
    private Intent intent = null;
    private ImageAdapter imageAdapter;
    private GoogleMap map;
    private Gson gson;
    private LatLng currentLatLng;
    private Marker lastMarker;
    private FirebaseFirestore db;
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

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(UpdateItemActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            inputBarcode.setText(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_item);

        db = FirebaseFirestore.getInstance();

        findViews();
        initViews();
        loadWarehouses();
        enableFields(false);

    }

    private void findViews() {
        inputBarcode = findViewById(R.id.input_barcode);
        inputName = findViewById(R.id.input_name);
        inputSupplier = findViewById(R.id.input_supplier);
        inputDescription = findViewById(R.id.input_description);
        inputQuantity = findViewById(R.id.input_quantity);
        inputLocation = findViewById(R.id.input_location);
        buttonScanBarcode = findViewById(R.id.button_scan_barcode);
        buttonUpdateItem = findViewById(R.id.button_update_item);
        buttonAttachImages = findViewById(R.id.button_attach_images);
        buttonCaptureImage = findViewById(R.id.button_capture_image);
        recyclerImages = findViewById(R.id.recycler_images);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        spinnerWarehouse = findViewById(R.id.spinner_warehouse);
        customNestedScrollView = findViewById(R.id.custom_nested_scroll_view);
        buttonSearch = findViewById(R.id.button_search);
        buttonReset = findViewById(R.id.button_reset);
    }

    private void initViews() {
        inputLocation.setEnabled(false);
        gson = new Gson();
        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this);
        recyclerImages.setAdapter(imageAdapter);
        addedImages=new ArrayList<>();

        buttonSearch.setOnClickListener(v -> searchItem());
        buttonUpdateItem.setOnClickListener(v -> checkAndUpdateItem());
        buttonReset.setOnClickListener(v -> {
            enableFields(false);
            depopulateFields();
            deleteImageFromFirebase(addedImages);
            isItemFound = false;
        });

        buttonScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

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
                    intent = new Intent(UpdateItemActivity.this, InventoryActivity.class);
                } else if (id == R.id.navigation_account) {
                    intent = new Intent(UpdateItemActivity.this, AccountActivity.class);
                } else if (id == R.id.navigation_reports) {
                    intent = new Intent(UpdateItemActivity.this, ReportsActivity.class);
                } else if (id == R.id.navigation_orders) {
                    intent = new Intent(UpdateItemActivity.this, OrdersActivity.class);
                } else if (id == R.id.navigation_home) {
                    intent = new Intent(UpdateItemActivity.this, HomeActivity.class);
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
        mapFragment.getMapAsync(this);

        // Disable scroll view while interacting with the map
    }

    private void searchItem() {
        String barcode = inputBarcode.getText() != null ? inputBarcode.getText().toString().trim() : "";
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";

        if (barcode.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please enter both barcode and name.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("items")
                .whereEqualTo("barcode", barcode)
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            currentItem = document.toObject(Item.class);
                            documentId = document.getId();
                            isItemFound = true;
                            populateFields(currentItem);
                            enableFields(true);
                            break;
                        }
                    } else {
                        Toast.makeText(UpdateItemActivity.this, "Item not found. Please check the barcode and name.", Toast.LENGTH_SHORT).show();
                        isItemFound = false;
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(UpdateItemActivity.this, "Failed to search item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void populateFields(Item item) {
        inputDescription.setText(item.getDescription());
        inputQuantity.setText(String.valueOf(item.getQuantity()));
        inputSupplier.setText(item.getSupplier());
        currentLatLng = new LatLng(item.getLatitude(), item.getLongitude());
        inputLocation.setText(item.getLatitude()+", "+item.getLongitude());
        enableFieldsMap(true);
        lastMarker = map.addMarker(new MarkerOptions().position(currentLatLng));
        String warehouseName = item.getWarehouseName();
        for (int i = 0; i < warehouseList.size(); i++) {
            if (warehouseList.get(i).getName().equals(warehouseName)) {
                spinnerWarehouse.setSelection(i);
                break;
            }
        }
        for (String url : item.getImageUrls()) {
            imageAdapter.addDefaultImage(UUID.randomUUID().toString(), Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.loading_gif)); // Add default image with unique ID
            Uri uri = Uri.parse(url);
            imageAdapter.updateImageUri(imageAdapter.getLastItemId(), uri, uri.toString());
        }
    }

    private void enableFields(boolean enabled) {
        inputDescription.setEnabled(enabled);
        inputQuantity.setEnabled(enabled);
        inputSupplier.setEnabled(enabled);
        inputLocation.setEnabled(enabled);
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

    private void depopulateFields() {
        inputDescription.setText("");
        inputQuantity.setText("");
        inputSupplier.setText("");
        imageAdapter.clear();
    }

    private void handleReturn() {
        if (imagesToUploadCount == imagesUploadedCount) {
            imageAdapter.removeAllImages();
            Intent intent = new Intent(UpdateItemActivity.this, InventoryActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Please wait for all images to be uploaded", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMinZoomPreference(15);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        }
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setOnMapClickListener(latLng -> {
            enableFieldsMap(isItemFound);
            if(isItemFound) {
                if (lastMarker != null) {
                    lastMarker.remove();
                }
                currentLatLng = latLng;
                inputLocation.setText(currentLatLng.latitude + ", " + currentLatLng.longitude);
                lastMarker = map.addMarker(new MarkerOptions().position(currentLatLng));
            }
        });
        map.setOnCameraMoveStartedListener(reason -> customNestedScrollView.setScrollingEnabled(false));
        map.setOnCameraIdleListener(() -> customNestedScrollView.setScrollingEnabled(true));
        // Set initial state of map interactions based on whether an item is found
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
                        Toast.makeText(UpdateItemActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        imagesToUploadCount--;
                    });
        }
    }

    private void checkAndUpdateItem() {
        if(currentLatLng==null)
        {
            Toast.makeText(this, "Please pick location", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imagesToUploadCount != imagesUploadedCount) {
            Toast.makeText(this, "Images still uploading please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        String barcode = inputBarcode.getText() != null ? inputBarcode.getText().toString().trim() : "";
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String description = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
        String quantityStr = inputQuantity.getText() != null ? inputQuantity.getText().toString().trim() : "";
        String supplier = inputSupplier.getText() != null ? inputSupplier.getText().toString().trim() : "";

        if (barcode.isEmpty() || name.isEmpty() || description.isEmpty() || quantityStr.isEmpty() || supplier.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveItem(barcode, name, description, quantityStr, supplier);
    }

    private void saveItem(String barcode, String name, String description, String quantityStr, String supplier) {
        double latitude = currentLatLng.latitude;
        double longitude = currentLatLng.longitude;

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLocationInsideWarehouse(currentLatLng)) {
            Item updatedItem = new Item(barcode, name, description, quantity, latitude, longitude, imageAdapter.getImageUrls(), true, selectedWarehouse.getName(), supplier, new Date());

            db.collection("items").document(documentId).set(updatedItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(UpdateItemActivity.this, "Item saved", Toast.LENGTH_SHORT).show();
                        doneSuccessfully = true;
                        addedImages.clear();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Location must be inside the selected warehouse", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isLocationInsideWarehouse(LatLng location) {
        if (selectedWarehouse == null || selectedWarehouse.getPoints() == null || selectedWarehouse.getPoints().size() < 3) {
            return false;
        }

        int crossings = 0;
        List<LatLng> points = selectedWarehouse.getPoints();
        for (int i = 0; i < points.size(); i++) {
            LatLng a = points.get(i);
            LatLng b = points.get((i + 1) % points.size());

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
            ax = b.longitude;
            ay = b.latitude;
            bx = a.longitude;
            by = a.latitude;
        }

        if (py == ay || py == by) {
            py += 0.00000001;
        }

        if (py > by || py < ay || px > Math.max(ax, bx)) {
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

        map.clear();
        PolygonOptions polygonOptions = new PolygonOptions();
        List<LatLng> sortedPoints = sortPoints(warehouse.getPoints());
        for (LatLng point : sortedPoints) {
            polygonOptions.add(point);
        }
        map.addPolygon(polygonOptions);
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
        db.collection("warehouses")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    warehouseList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = (String) document.get("name");

                        Type listType = new TypeToken<ArrayList<LatLng>>() {}.getType();
                        ArrayList<LatLng> points = gson.fromJson(gson.toJson(document.get("points")), listType);
                        warehouseList.add(new Warehouse(name, points, true));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getWarehouseNames());
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerWarehouse.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load warehouses: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        if(urls==null)return;
        for (String url: urls) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            imageRef.delete().addOnSuccessListener(aVoid -> {
            }).addOnFailureListener(e -> {
                Toast.makeText(UpdateItemActivity.this, "Failed to reset images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }
        urls.clear();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            if (imagesUploadedCount > 0 && !doneSuccessfully) {
                if(!addedImages.isEmpty()) {
                    deleteImageFromFirebase(addedImages);
                    Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
