package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Item implements Parcelable {
    public static final int MIN_LINES_COLLAPSED = 3;
    private String barcode;
    private String name;
    private String description;
    private int quantity;
    private double latitude;
    private double longitude;
    private List<String> imageUrls;
    private boolean collapsed;

    public Item() {
        // Default constructor required for calls to DataSnapshot.getValue(Item.class)
    }

    public Item(String barcode, String name, String description, int quantity, double latitude, double longitude, List<String> imageUrls) {
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrls = imageUrls;
        this.collapsed = true; // Default to collapsed state
    }

    protected Item(Parcel in) {
        barcode = in.readString();
        name = in.readString();
        description = in.readString();
        quantity = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        imageUrls = in.createStringArrayList();
        collapsed = in.readByte() != 0;
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(barcode);
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeInt(quantity);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeStringList(imageUrls);
        parcel.writeByte((byte) (collapsed ? 1 : 0));
    }
}
