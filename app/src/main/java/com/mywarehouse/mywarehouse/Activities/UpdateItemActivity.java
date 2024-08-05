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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mywarehouse.mywarehouse.Adapters.ImageAdapter;
import com.mywarehouse.mywarehouse.Adapters.WarehouseAdapter;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseUpdateItem;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.CustomNestedScrollView;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

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
    private TextInputEditText inputSupplier, inputBarcode, inputName, inputDescription;
    private MaterialButton buttonReset, buttonSearch, buttonUpdateItem;
    private AppCompatImageButton buttonScanBarcode, buttonAttachImages, buttonCaptureImage;
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
    private Gson gson;
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
    private Polygon lastpolygon;

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
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
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
        buttonScanBarcode = findViewById(R.id.button_scan_barcode);
        buttonUpdateItem = findViewById(R.id.button_update_item);
        buttonAttachImages = findViewById(R.id.button_attach_images);
        buttonCaptureImage = findViewById(R.id.button_capture_image);
        recyclerImages = findViewById(R.id.recycler_images);
        recyclerWarehouses = findViewById(R.id.recycler_warehouses);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        spinnerWarehouse = findViewById(R.id.spinner_warehouse);
        customNestedScrollView = findViewById(R.id.custom_nested_scroll_view);
        buttonSearch = findViewById(R.id.button_search);
        buttonReset = findViewById(R.id.button_reset);
    }

    private void initViews() {
        gson = new Gson();
        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this);
        recyclerImages.setAdapter(imageAdapter);
        addedImages = new ArrayList<>();

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

        recyclerWarehouses.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        warehouseAdapter = new WarehouseAdapter(itemWarehouseList);
        recyclerWarehouses.setAdapter(warehouseAdapter);
    }

    private void searchItem() {
        String barcode = inputBarcode.getText() != null ? inputBarcode.getText().toString().trim() : "";
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";

        if (barcode.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please enter both barcode and name.", Toast.LENGTH_SHORT).show();
            return;
        }

        documentId = barcode + "_" + name;
        FirebaseUpdateItem.fetchItem(documentId, new FirebaseUpdateItem.FirestoreCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                currentItem = item;
                isItemFound = true;
                // Check if the adapter is already set, if not, populateFields will be called after the adapter is set in loadWarehouses
                if (spinnerWarehouse.getAdapter() != null) {
                    populateFields(currentItem);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(UpdateItemActivity.this, "Failed to search item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isItemFound = false;
            }
        });
    }

    private void populateFields(Item item) {
        inputDescription.setText(item.getDescription());
        inputSupplier.setText(item.getSupplier());
        enableFieldsMap(true);
        enableFields(true);

        // Clear the map and add the marker for the item's location
        itemWarehouseList.addAll(item.getItemWarehouses());
        warehouseAdapter.notifyDataSetChanged();
        if (map != null) {
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


        for (String url : item.getImageUrls()) {
            imageAdapter.addDefaultImage(UUID.randomUUID().toString(), Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.loading_gif)); // Add default image with unique ID
            Uri uri = Uri.parse(url);
            imageAdapter.updateImageUri(imageAdapter.getLastItemId(), uri, uri.toString());
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

    private void depopulateFields() {
        inputDescription.setText("");
        inputSupplier.setText("");
        imageAdapter.clear();
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
                        Toast.makeText(UpdateItemActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        imagesToUploadCount--;
                    });
        }
    }

    private void checkAndUpdateItem() {
        if (imagesToUploadCount != imagesUploadedCount) {
            Toast.makeText(this, "Images still uploading please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        String barcode = inputBarcode.getText() != null ? inputBarcode.getText().toString().trim() : "";
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String description = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
        String supplier = inputSupplier.getText() != null ? inputSupplier.getText().toString().trim() : "";

        if (barcode.isEmpty() || name.isEmpty() || description.isEmpty() ||  supplier.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (ItemWarehouse itemWarehouse : itemWarehouseList) {
            if (itemWarehouse.getQuantity() <= 0) {
                Toast.makeText(this, "Each quantity must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        saveItem(barcode, name,description,supplier);
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
                        saveLog(updatedItem, currentItem);

                        FirebaseUpdateItem.saveItem(documentId, updatedItem, new FirebaseUpdateItem.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(UpdateItemActivity.this, "Item saved", Toast.LENGTH_SHORT).show();
                                saveOutOfStockLog(updatedItem.getName(), updatedItem.getBarcode(), currentDate);

                                doneSuccessfully = true;
                                addedImages.clear();
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(UpdateItemActivity.this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            saveLog(updatedItem, currentItem);

            FirebaseUpdateItem.saveItem(documentId, updatedItem, new FirebaseUpdateItem.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(UpdateItemActivity.this, "Item saved", Toast.LENGTH_SHORT).show();

                    doneSuccessfully = true;
                    addedImages.clear();
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(UpdateItemActivity.this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    private void saveOutOfStockLog(String itemName, String barcode, Date date) {
        String invokedBy = MyUser.getInstance().getName();
        String notes = "Item " + itemName + " is out of stock because it was updated during a warehouse inventory check or it has run out due to orders.";
        MyLog myLog = new MyLog("Item out of stock", date, notes, invokedBy, LogType.OUT_OF_STOCK);

        FirebaseUpdateItem.saveLog(myLog, new FirebaseUpdateItem.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Log saved successfully
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(UpdateItemActivity.this, "Failed to save log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLog(Item newItem, Item oldItem) {
        String invokedBy = MyUser.getInstance().getName();
        StringBuilder notes = new StringBuilder("Item " + newItem.getName() + " has been updated. Changes:\n");

        if (!newItem.getDescription().equals(oldItem.getDescription())) {
            notes.append("Description changed from '").append(oldItem.getDescription()).append("' to '").append(newItem.getDescription()).append("'\n");
        }
        if (newItem.getTotalQuantity() != oldItem.getTotalQuantity()) {
            notes.append("Quantity changed from ").append(oldItem.getTotalQuantity()).append(" to ").append(newItem.getTotalQuantity()).append("\n");
        }
        if (!newItem.getSupplier().equals(oldItem.getSupplier())) {
            notes.append("Supplier changed from '").append(oldItem.getSupplier()).append("' to '").append(newItem.getSupplier()).append("'\n");
        }
        boolean flag = true;
        int countNew = newItem.getItemWarehouses().size();
        int countOld = oldItem.getItemWarehouses().size();
        if (countNew != countOld) {
            flag = false;
        } else {
            for (ItemWarehouse itemWarehouse : newItem.getItemWarehouses()) {
                for (ItemWarehouse itemWarehouseOld : oldItem.getItemWarehouses()) {
                    if (itemWarehouseOld.getWarehouseName().equals(itemWarehouse.getWarehouseName())) {
                        countOld--;
                        countNew--;
                        break;
                    }
                }
            }
            if (countNew != 0 || countOld != 0) flag = false;
        }

        if (!flag) {
            if (newItem.getItemWarehouses().isEmpty()) notes.append("Warehouses: no present location, OUT OF STOCK");
            else {
                notes.append("Warehouses changed").append(" to ").append("\n");
                for (ItemWarehouse itemWarehouse : newItem.getItemWarehouses()) {
                    notes.append("- Warehouse: ").append(itemWarehouse.getWarehouseName()).append("- Qty: ").append(itemWarehouse.getQuantity()).append("\n");
                }
            }
        }
        if (!newItem.getImageUrls().equals(oldItem.getImageUrls())) {
            notes.append("Images updated.\n");
        }

        MyLog myLog = new MyLog("Item modification", new Date(), notes.toString(), invokedBy, LogType.ITEM_MODIFICATION);
        FirebaseUpdateItem.saveLog(myLog, new FirebaseUpdateItem.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Log saved successfully
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(UpdateItemActivity.this, "Failed to save log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        FirebaseUpdateItem.fetchWarehouses(new FirebaseUpdateItem.FirestoreCallback<List<Warehouse>>() {
            @Override
            public void onSuccess(List<Warehouse> warehouses) {
                warehouseList.clear();
                warehouseList.addAll(warehouses);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(UpdateItemActivity.this, android.R.layout.simple_spinner_item, getWarehouseNames());
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerWarehouse.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(UpdateItemActivity.this, "Failed to load warehouses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                if (!addedImages.isEmpty()) {
                    deleteImageFromFirebase(addedImages);
                    Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
