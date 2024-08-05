package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.mywarehouse.mywarehouse.Enums.OrderType;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Order implements Parcelable {
    private List<PickupItem> pickupItems;
    private String createdBy;
    private String orderId;
    private Date orderDate;
    private OrderType status;
    private String collectedBy;
    private String selectedWarehouse;

    public Order() {
    }

    public Order(List<PickupItem> pickupItems, String createdBy, String orderId, Date orderDate, OrderType status, String collectedBy, String selectedWarehouse) {
        this.pickupItems = pickupItems;
        this.createdBy = createdBy;
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.status = status;
        this.collectedBy = collectedBy;
        this.selectedWarehouse = selectedWarehouse;
    }

    protected Order(Parcel in) {
        pickupItems = in.createTypedArrayList(PickupItem.CREATOR);
        createdBy = in.readString();
        orderId = in.readString();
        long tmpOrderDate = in.readLong();
        orderDate = tmpOrderDate == -1 ? null : new Date(tmpOrderDate);
        status = OrderType.valueOf(in.readString());
        collectedBy = in.readString();
        selectedWarehouse = in.readString();
    }

    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel in) {
            return new Order(in);
        }

        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };

    public List<PickupItem> getPickupItems() {
        return pickupItems;
    }

    public void setPickupItems(List<PickupItem> pickupItems) {
        this.pickupItems = pickupItems;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public OrderType getStatus() {
        return status;
    }

    public void setStatus(OrderType status) {
        this.status = status;
    }

    public String getCollectedBy() {
        return collectedBy;
    }

    public void setCollectedBy(String collectedBy) {
        this.collectedBy = collectedBy;
    }

    public String getSelectedWarehouse() {
        return selectedWarehouse;
    }

    public void setSelectedWarehouse(String selectedWarehouse) {
        this.selectedWarehouse = selectedWarehouse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return orderId.equals(order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(pickupItems);
        dest.writeString(createdBy);
        dest.writeString(orderId);
        dest.writeLong(orderDate != null ? orderDate.getTime() : -1);
        dest.writeString(status.name());
        dest.writeString(collectedBy);
        dest.writeString(selectedWarehouse);
    }
}
