package com.mywarehouse.mywarehouse.Adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.R;

import java.util.List;

public class WarehouseAdapter extends RecyclerView.Adapter<WarehouseAdapter.WarehouseViewHolder> {

    private List<ItemWarehouse> itemWarehouseList;

    public WarehouseAdapter(List<ItemWarehouse> itemWarehouseList) {
        this.itemWarehouseList = itemWarehouseList;
    }

    @NonNull
    @Override
    public WarehouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_warehouse, parent, false);
        return new WarehouseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WarehouseViewHolder holder, int position) {
        ItemWarehouse itemWarehouse = itemWarehouseList.get(position);
        holder.warehouseName.setText(itemWarehouse.getWarehouseName());

        holder.location.setText(itemWarehouse.getLocation().latitude+","+itemWarehouse.getLocation().longitude);
        holder.quantity.setText(String.valueOf(itemWarehouse.getQuantity()));
        holder.quantity.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    itemWarehouse.setQuantity(Integer.parseInt(holder.quantity.getText().toString()));
                    if(itemWarehouse.getQuantity()<0)itemWarehouse.setQuantity(0);
                } catch (NumberFormatException e) {
                    itemWarehouse.setQuantity(0);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemWarehouseList.size();
    }

    public static class WarehouseViewHolder extends RecyclerView.ViewHolder {
        TextView warehouseName;
        TextView location;
        EditText quantity;

        public WarehouseViewHolder(@NonNull View itemView) {
            super(itemView);
            warehouseName = itemView.findViewById(R.id.text_warehouse);
            location = itemView.findViewById(R.id.text_location);
            quantity = itemView.findViewById(R.id.edit_quantity);
        }
    }
}
