package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class PickupItem implements Parcelable {
    private String name;
    private String barcode;
    private int quantity;

    // Default constructor required for calls to DataSnapshot.getValue(PickupItem.class)
    public PickupItem() {}

    public PickupItem(String name, String barcode, int quantity) {
        this.name = name;
        this.barcode = barcode;
        this.quantity = quantity;
    }

    protected PickupItem(Parcel in) {
        name = in.readString();
        barcode = in.readString();
        quantity = in.readInt();
    }

    public static final Creator<PickupItem> CREATOR = new Creator<PickupItem>() {
        @Override
        public PickupItem createFromParcel(Parcel in) {
            return new PickupItem(in);
        }

        @Override
        public PickupItem[] newArray(int size) {
            return new PickupItem[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(barcode);
        dest.writeInt(quantity);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PickupItem that = (PickupItem) o;

        if (!name.equals(that.name)) return false;
        return barcode.equals(that.barcode);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + barcode.hashCode();
        return result;
    }
}
