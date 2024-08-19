package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionRequest implements Parcelable {
    private String requestId;
    private String Warehouse;
    private String orderId;
    private String CreatedBy;
    private List<PickupItem> requestedItemsToMove;
    private boolean onUpdate; // Added field

    public TransactionRequest() {
    }

    public TransactionRequest(String requestId, String warehouse, String orderId, String createdBy, List<PickupItem> requestedItemsToMove, boolean onUpdate) {
        this.requestId = requestId;
        Warehouse = warehouse;
        this.orderId = orderId;
        CreatedBy = createdBy;
        this.requestedItemsToMove = requestedItemsToMove;
        this.onUpdate = onUpdate; // Initialize the new field
    }

    protected TransactionRequest(Parcel in) {
        requestId = in.readString();
        Warehouse = in.readString();
        orderId = in.readString();
        CreatedBy = in.readString();
        requestedItemsToMove = in.createTypedArrayList(PickupItem.CREATOR);
        onUpdate = in.readByte() != 0; // Read the new field
    }

    public static final Creator<TransactionRequest> CREATOR = new Creator<TransactionRequest>() {
        @Override
        public TransactionRequest createFromParcel(Parcel in) {
            return new TransactionRequest(in);
        }

        @Override
        public TransactionRequest[] newArray(int size) {
            return new TransactionRequest[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(requestId);
        dest.writeString(Warehouse);
        dest.writeString(orderId);
        dest.writeString(CreatedBy);
        dest.writeTypedList(requestedItemsToMove);
        dest.writeByte((byte) (onUpdate ? 1 : 0)); // Write the new field
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getWarehouse() {
        return Warehouse;
    }

    public void setWarehouse(String warehouse) {
        Warehouse = warehouse;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCreatedBy() {
        return CreatedBy;
    }

    public void setCreatedBy(String createdBy) {
        CreatedBy = createdBy;
    }

    public List<PickupItem> getRequestedItemsToMove() {
        return requestedItemsToMove;
    }

    public void setRequestedItemsToMove(List<PickupItem> requestedItemsToMove) {
        this.requestedItemsToMove = requestedItemsToMove;
    }

    public boolean isOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(boolean onUpdate) {
        this.onUpdate = onUpdate;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionRequest that = (TransactionRequest) o;
        return onUpdate == that.onUpdate &&
                Objects.equals(requestId, that.requestId) &&
                Objects.equals(Warehouse, that.Warehouse) &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(CreatedBy, that.CreatedBy) &&
                Objects.equals(requestedItemsToMove, that.requestedItemsToMove);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, Warehouse, orderId, CreatedBy, requestedItemsToMove, onUpdate);
    }
}
