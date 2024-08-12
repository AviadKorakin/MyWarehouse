package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Activities.AcceptTransactionActivity;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Interfaces.DataLoadCallback;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.Models.WarehouseKey;
import com.mywarehouse.mywarehouse.Models.WarehouseQuantityRange;
import com.mywarehouse.mywarehouse.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcceptTransactionAdapter extends RecyclerView.Adapter<AcceptTransactionAdapter.AcceptTransactionViewHolder> {

    private Context context;
    private List<PickupItem> pickupItemList;
    private TransactionRequest transactionRequest;
    private Map<PickupItem, List<WarehouseQuantityRange>> warehouseQuantityMap = new HashMap<>();
    private Map<PickupItem, List<String>> itemImagesMap = new HashMap<>();
    private DataLoadCallback dataLoadCallback;
    private PickupItem currentSelectedPickupItem;
    private Map<PickupItem, WarehouseQuantityRange> selectedWarehouseQuantityRangeMap = new HashMap<>();

    public AcceptTransactionAdapter(Context context, List<PickupItem> pickupItemList, TransactionRequest transactionRequest, DataLoadCallback dataLoadCallback) {
        this.context = context;
        this.pickupItemList = pickupItemList;
        this.transactionRequest = transactionRequest;
        this.dataLoadCallback = dataLoadCallback;
        fetchItemsAndPopulateWarehouseQuantityMap();
    }

    @NonNull
    @Override
    public AcceptTransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_accept_transaction, parent, false);
        return new AcceptTransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AcceptTransactionViewHolder holder, int position) {
        PickupItem pickupItem = pickupItemList.get(position);
        holder.itemBarcode.setText(pickupItem.getBarcode());
        holder.itemName.setText(pickupItem.getName());
        holder.destinationWarehouse.setText(transactionRequest.getWarehouse()); // Set the destination warehouse

        // Populate spinner and set default quantities
        populateSpinnerAndSetQuantities(pickupItem, holder);

        // Handle quantity adjustments
        holder.buttonPlus.setOnClickListener(v -> adjustQuantity(holder, pickupItem, 1));
        holder.buttonMinus.setOnClickListener(v -> adjustQuantity(holder, pickupItem, -1));

        // Set up the image recycler view
        setupImageRecyclerView(holder.recyclerViewImages, pickupItem);

        // Handle map button click
        holder.buttonMap.setOnClickListener(v -> {
            // Set the current selected pickup item
            currentSelectedPickupItem = pickupItem;
            // Center the map on the warehouse
            ((AcceptTransactionActivity) context).focusOnWarehouse();
            ((AcceptTransactionActivity) context).editingModeMap.put(pickupItem, true);// Enable editing mode for the item
        });
    }

    public List<PickupItem> getPickupItemList() {
        return pickupItemList;
    }

    public PickupItem getCurrentSelectedPickupItem() {
        return currentSelectedPickupItem;
    }

    public Map<PickupItem, WarehouseQuantityRange> getSelectedWarehouseQuantityRangeMap() {
        return selectedWarehouseQuantityRangeMap;
    }

    private void fetchItemsAndPopulateWarehouseQuantityMap() {
        for (PickupItem pickupItem : pickupItemList) {
            String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
            FirebaseForAdapters.fetchItem(documentId, item -> {
                // Set onUpdate to true
                FirebaseForAdapters.updateItemOnUpdateStatus(documentId, true, new FirebaseForAdapters.FirestoreCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(Exception e) {

                    }
                });

                List<WarehouseQuantityRange> warehouseQuantities = new ArrayList<>();
                int warehouseCounter = 1;
                List<ItemWarehouse> sortedList=item.getItemWarehouses();
                Collections.sort(sortedList);
                for (ItemWarehouse itemWarehouse : sortedList) {
                    if (itemWarehouse.getQuantity() >= pickupItem.getQuantity()) {
                        String uniqueWarehouseName = "(" + warehouseCounter++ + ")" + itemWarehouse.getWarehouseName();
                        WarehouseKey warehouseKey = new WarehouseKey(uniqueWarehouseName, itemWarehouse);
                        warehouseQuantities.add(new WarehouseQuantityRange(warehouseKey, pickupItem.getQuantity(), itemWarehouse.getQuantity()));
                    }
                }
                warehouseQuantityMap.put(pickupItem, warehouseQuantities);
                itemImagesMap.put(pickupItem, item.getImageUrls());

                // Check if all items have been loaded
                if (warehouseQuantityMap.size() == pickupItemList.size()) {
                    dataLoadCallback.onDataLoaded();
                }
            });
        }
    }

    private void populateSpinnerAndSetQuantities(PickupItem pickupItem, AcceptTransactionViewHolder holder) {
        List<WarehouseQuantityRange> warehouseQuantities = warehouseQuantityMap.get(pickupItem);
        if (warehouseQuantities != null) {
            List<String> warehouseNames = new ArrayList<>();
            for (WarehouseQuantityRange range : warehouseQuantities) {
                warehouseNames.add(range.getWarehouseKey().getWarehouseName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item, warehouseNames);
            adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
            holder.sourceWarehouseSpinner.setAdapter(adapter);

            holder.sourceWarehouseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedWarehouseName = warehouseNames.get(position);
                    WarehouseQuantityRange selectedRange = null;
                    for (WarehouseQuantityRange range : warehouseQuantities) {
                        if (range.getWarehouseKey().getWarehouseName().equals(selectedWarehouseName)) {
                            selectedRange = range;
                            break;
                        }
                    }
                    selectedWarehouseQuantityRangeMap.put(pickupItem, selectedRange);
                    updateQuantitiesBasedOnWarehouse(pickupItem, selectedWarehouseName, holder);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });

            // Set default quantity based on the first warehouse in the list
            holder.itemQuantity.setText(String.valueOf(warehouseQuantities.get(0).getMinQuantity()));
        }
    }

    private void updateQuantitiesBasedOnWarehouse(PickupItem pickupItem, String selectedWarehouse, AcceptTransactionViewHolder holder) {
        List<WarehouseQuantityRange> warehouseQuantities = warehouseQuantityMap.get(pickupItem);
        if (warehouseQuantities != null) {
            for (WarehouseQuantityRange range : warehouseQuantities) {
                if (range.getWarehouseKey().getWarehouseName().equals(selectedWarehouse)) {
                    holder.itemQuantity.setText(String.valueOf(range.getMinQuantity()));
                    range.setDesiredQuantity(range.getMinQuantity()); // Update desired quantity
                    break;
                }
            }
        }
    }

    private void adjustQuantity(AcceptTransactionViewHolder holder, PickupItem pickupItem, int adjustment) {
        int currentQuantity = Integer.parseInt(holder.itemQuantity.getText().toString());
        String selectedWarehouse = (String) holder.sourceWarehouseSpinner.getSelectedItem();
        List<WarehouseQuantityRange> warehouseQuantities = warehouseQuantityMap.get(pickupItem);

        if (warehouseQuantities != null) {
            for (WarehouseQuantityRange range : warehouseQuantities) {
                if (range.getWarehouseKey().getWarehouseName().equals(selectedWarehouse)) {
                    int newQuantity = currentQuantity + adjustment;
                    if (newQuantity >= range.getMinQuantity() && newQuantity <= range.getMaxQuantity()) {
                        holder.itemQuantity.setText(String.valueOf(newQuantity));
                        range.setDesiredQuantity(newQuantity); // Update desired quantity
                    }
                    break;
                }
            }
        }
    }

    private void setupImageRecyclerView(RecyclerView recyclerView, PickupItem pickupItem) {
        List<String> imageUrls = itemImagesMap.get(pickupItem);
        ImageShowAdapter imageShowAdapter = new ImageShowAdapter(context, imageUrls);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(imageShowAdapter);
    }

    @Override
    public int getItemCount() {
        return pickupItemList.size();
    }

    public static class AcceptTransactionViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView itemBarcode, itemName, itemQuantity, destinationWarehouse;
        AppCompatImageButton buttonPlus, buttonMinus, buttonMap;
        Spinner sourceWarehouseSpinner;
        RecyclerView recyclerViewImages;

        public AcceptTransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            itemBarcode = itemView.findViewById(R.id.item_barcode);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            buttonPlus = itemView.findViewById(R.id.button_plus);
            buttonMinus = itemView.findViewById(R.id.button_minus);
            sourceWarehouseSpinner = itemView.findViewById(R.id.source_warehouse_spinner);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_images);
            destinationWarehouse = itemView.findViewById(R.id.destination_warehouse);
            buttonMap = itemView.findViewById(R.id.button_map); // Map button
        }
    }
}
