package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.mywarehouse.mywarehouse.R;

public class RoleSpinnerAdapter extends BaseAdapter {

    private Context context;
    private String[] roles;
    private int[] icons;
    private LayoutInflater inflater;

    public RoleSpinnerAdapter(Context context, String[] roles, int[] icons) {
        this.context = context;
        this.roles = roles;
        this.icons = icons;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return roles.length;
    }

    @Override
    public Object getItem(int position) {
        return roles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_role_spinner, parent, false);
        AppCompatImageView icon = view.findViewById(R.id.icon);
        AppCompatTextView text = view.findViewById(R.id.text);

        icon.setImageResource(icons[position]);
        text.setText(roles[position]);

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
