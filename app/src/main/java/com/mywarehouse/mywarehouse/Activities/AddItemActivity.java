package com.mywarehouse.mywarehouse.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mywarehouse.mywarehouse.Adapters.ImageAdapter;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class AddItemActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextInputEditText inputBarcode, inputName, inputDescription, inputQuantity, inputLocation;
    private MaterialButton buttonScanBarcode, buttonSaveItem, buttonAttachImages, buttonCaptureImage;
    private RecyclerView recyclerImages;
    private BottomNavigationView bottomNavigationView;
    private Intent intent = null;
    private ImageAdapter imageAdapter;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private LatLng currentLatLng;
    private FirebaseFirestore db;
    private String currentPhotoPath;
    private int imagesToUploadCount = 0;
    private int imagesUploadedCount = 0;
    private boolean doneSuccessfully = false;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(AddItemActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            inputBarcode.setText(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        findViews();
        initViews();
    }

    private void findViews() {
        inputBarcode = findViewById(R.id.input_barcode);
        inputName = findViewById(R.id.input_name);
        inputDescription = findViewById(R.id.input_description);
        inputQuantity = findViewById(R.id.input_quantity);
        inputLocation = findViewById(R.id.input_location);
        buttonScanBarcode = findViewById(R.id.button_scan_barcode);
        buttonSaveItem = findViewById(R.id.button_save_item);
        buttonAttachImages = findViewById(R.id.button_attach_images);
        buttonCaptureImage = findViewById(R.id.button_capture_image);
        recyclerImages = findViewById(R.id.recycler_images);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void initViews() {
        inputLocation.setEnabled(false);

        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this);
        recyclerImages.setAdapter(imageAdapter);

        buttonScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        buttonAttachImages.setOnClickListener(v -> openFileChooser());

        buttonCaptureImage.setOnClickListener(v -> captureImage());

        buttonSaveItem.setOnClickListener(v -> saveItem());

        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);

        bottomNavigationView.setSelectedItemId(R.id.navigation_inventory);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (imagesToUploadCount == imagesUploadedCount) {
                int id = item.getItemId();
                if (id == R.id.navigation_inventory) {
                    intent = new Intent(AddItemActivity.this, InventoryActivity.class);
                } else if (id == R.id.navigation_account) {
                    intent = new Intent(AddItemActivity.this, AccountActivity.class);
                } else if (id == R.id.navigation_reports) {
                    intent = new Intent(AddItemActivity.this, ReportsActivity.class);
                } else if (id == R.id.navigation_orders) {
                    intent = new Intent(AddItemActivity.this, OrdersActivity.class);
                } else if (id == R.id.navigation_home) {
                    intent = new Intent(AddItemActivity.this, HomeActivity.class);
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

        getLocationPermission();
    }

    private void handleReturn() {
        if (imagesToUploadCount == imagesUploadedCount) {
            imageAdapter.removeAllImages();
            Intent intent = new Intent(AddItemActivity.this, InventoryActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Please wait for all images to be uploaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastKnownLocation();
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        inputLocation.setText(currentLatLng.latitude + ", " + currentLatLng.longitude);
                        if (map != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                            map.addMarker(new MarkerOptions().position(currentLatLng));
                        }
                    } else {
                        Toast.makeText(AddItemActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMaxZoomPreference(17);
        map.setMinZoomPreference(15);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        if (currentLatLng != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
            map.addMarker(new MarkerOptions().position(currentLatLng));
        }
        map.setOnMapClickListener(latLng -> {
            currentLatLng = latLng;
            inputLocation.setText(currentLatLng.latitude + ", " + currentLatLng.longitude);
            map.clear();
            map.addMarker(new MarkerOptions().position(currentLatLng));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
        });
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
                    imagesToUploadCount++;
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
                        imageAdapter.updateImageUri(imageId, uri, uri.toString());
                        if (imagesToUploadCount == imagesUploadedCount) {
                            bottomNavigationView.setVisibility(View.VISIBLE);
                        }
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddItemActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        imagesToUploadCount--;
                    });
        }
    }

    private void saveItem() {
        if (Objects.requireNonNull(inputBarcode.getText()).toString().trim().isEmpty() || Objects.requireNonNull(inputName.getText()).toString().trim().isEmpty() ||
                Objects.requireNonNull(inputDescription.getText()).toString().trim().isEmpty() || Objects.requireNonNull(inputQuantity.getText()).toString().trim().isEmpty() ||
                Objects.requireNonNull(inputLocation.getText()).toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imagesToUploadCount == imagesUploadedCount) {
            saveItemToDatabase();
        } else {
            Toast.makeText(this, "Please wait for all images to be uploaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveItemToDatabase() {
        String barcode = Objects.requireNonNull(inputBarcode.getText()).toString().trim();
        String name = Objects.requireNonNull(inputName.getText()).toString().trim();
        String description = Objects.requireNonNull(inputDescription.getText()).toString().trim();
        String quantityStr = Objects.requireNonNull(inputQuantity.getText()).toString().trim();
        double latitude =   currentLatLng.latitude;
        double longitude = currentLatLng.longitude;

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        Item item = new Item(barcode, name, description, quantity, latitude, longitude, imageAdapter.getImageUrls());

        db.collection("items").add(item)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddItemActivity.this, "Item saved", Toast.LENGTH_SHORT).show();
                    imageAdapter.setItemId(documentReference.getId());
                    doneSuccessfully = true;
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            if (imagesUploadedCount > 0 && !doneSuccessfully) {
                imageAdapter.removeAllImages();
                Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
