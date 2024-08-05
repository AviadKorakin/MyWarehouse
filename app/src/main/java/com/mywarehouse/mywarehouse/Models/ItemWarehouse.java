package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class ItemWarehouse implements Parcelable ,Comparable<ItemWarehouse>{
    private String warehouseName;
    private MyLatLng location;
    private int quantity;

    public ItemWarehouse()
    {

    }
    public ItemWarehouse(String warehouseName, LatLng location, int quantity) {
        this.warehouseName = warehouseName;
        this.location=new MyLatLng(location.latitude,location.longitude);
        this.quantity = quantity;
    }

    protected ItemWarehouse(Parcel in) {
        warehouseName = in.readString();
        location = in.readParcelable(LatLng.class.getClassLoader());
        quantity = in.readInt();
    }

    public static final Creator<ItemWarehouse> CREATOR = new Creator<ItemWarehouse>() {
        @Override
        public ItemWarehouse createFromParcel(Parcel in) {
            return new ItemWarehouse(in);
        }

        @Override
        public ItemWarehouse[] newArray(int size) {
            return new ItemWarehouse[size];
        }
    };

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public MyLatLng getLocation() {
        return location;
    }

    public void setLocation(MyLatLng location) {
        this.location = location;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(warehouseName);
        dest.writeParcelable(location,0);
        dest.writeInt(quantity);
    }

    @Override
    public int compareTo(ItemWarehouse o) {
        return warehouseName.compareTo(o.warehouseName);
    }
}
