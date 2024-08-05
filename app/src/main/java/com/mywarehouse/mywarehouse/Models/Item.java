package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.List;

public class Item implements Parcelable {
    public static final int MIN_LINES_COLLAPSED = 1;
    private String barcode;
    private String name;
    private String description;
    private int totalQuantity;
    private List<String> imageUrls;
    private boolean active;
    private String supplier;
    private Date lastModified;
    private List<ItemWarehouse> itemWarehouses;
    private int requestedAmount;
    private boolean collapsed;


    public Item() {
        // Default constructor for Firebase
    }

    public Item(String barcode, String name, String description, int totalQuantity, List<String> imageUrls, boolean active, String supplier, Date lastModified, List<ItemWarehouse> itemWarehouses,int requestedAmount) {
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.totalQuantity = totalQuantity;
        this.imageUrls = imageUrls;
        this.active = active;
        this.supplier = supplier;
        this.lastModified = lastModified;
        this.itemWarehouses = itemWarehouses;
        this.requestedAmount=requestedAmount;
        this.collapsed = true;
    }

    protected Item(Parcel in) {
        barcode = in.readString();
        name = in.readString();
        description = in.readString();
        totalQuantity = in.readInt();
        imageUrls = in.createStringArrayList();
        active = in.readByte() != 0;
        supplier = in.readString();
        lastModified = new Date(in.readLong());
        itemWarehouses = in.createTypedArrayList(ItemWarehouse.CREATOR);
        requestedAmount=in.readInt();

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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(barcode);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeInt(totalQuantity);
        dest.writeStringList(imageUrls);
        dest.writeByte((byte) (active ? 1 : 0));
        dest.writeString(supplier);
        dest.writeLong(lastModified != null ? lastModified.getTime() : -1);
        dest.writeTypedList(itemWarehouses);
        dest.writeInt(requestedAmount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and Setters

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

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }


    public List<ItemWarehouse> getItemWarehouses() {
        return itemWarehouses;
    }

    public void setItemWarehouses(List<ItemWarehouse> itemWarehouses) {
        this.itemWarehouses = itemWarehouses;
    }
    public boolean isCollapsed() {
        return collapsed;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
    public int getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(int requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    @Override
    public String toString() {
        return "Item{" +
                "barcode='" + barcode + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", totalQuantity=" + totalQuantity +
                ", imageUrls=" + imageUrls +
                ", active=" + active +
                ", supplier='" + supplier + '\'' +
                ", createdAt=" + lastModified +
                ", itemWarehouses=" + itemWarehouses +
                '}';
    }
}
