package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;

import java.util.List;

public class WarehouseSpinnerAdapter extends ArrayAdapter<Warehouse> {
    private Context context;
    private List<Warehouse> warehouses;
    private int icon;

    public WarehouseSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<Warehouse> objects, int icon) {
        super(context, resource, objects);
        this.context = context;
        this.warehouses = objects;
        this.icon = icon;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.spinner_item_warehouse, parent, false);

        AppCompatTextView textView = view.findViewById(R.id.text_view_warehouse);
        AppCompatImageView imageView = view.findViewById(R.id.image_view_icon);

        textView.setText(warehouses.get(position).getName());
        imageView.setImageDrawable(ContextCompat.getDrawable(context, icon));

        return view;
    }
}
