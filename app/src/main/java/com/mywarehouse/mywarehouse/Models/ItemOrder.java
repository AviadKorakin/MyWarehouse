package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class ItemOrder extends Item implements Parcelable {
    private int selectedQuantity;

    public ItemOrder() {
        // Default constructor required for calls to DataSnapshot.getValue(ItemOrder.class)
    }

    public ItemOrder(Item item, int selectedQuantity) {
        super(item.getBarcode(), item.getName(), item.getDescription(), item.getTotalQuantity(), item.getImageUrls(), item.isActive(), item.getSupplier(), item.getLastModified(), item.getItemWarehouses(),item.getRequestedAmount());
        this.selectedQuantity = selectedQuantity;
    }

    public int getSelectedQuantity() {
        return selectedQuantity;
    }

    public void setSelectedQuantity(int selectedQuantity) {
        this.selectedQuantity = selectedQuantity;
    }

    protected ItemOrder(Parcel in) {
        super(in);
        selectedQuantity = in.readInt();
    }

    public static final Creator<ItemOrder> CREATOR = new Creator<ItemOrder>() {
        @Override
        public ItemOrder createFromParcel(Parcel in) {
            return new ItemOrder(in);
        }

        @Override
        public ItemOrder[] newArray(int size) {
            return new ItemOrder[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(selectedQuantity);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
