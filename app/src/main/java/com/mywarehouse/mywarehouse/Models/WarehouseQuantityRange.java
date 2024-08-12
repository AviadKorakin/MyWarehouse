package com.mywarehouse.mywarehouse.Models;

public class WarehouseQuantityRange {
    private WarehouseKey warehouseKey;
    private int minQuantity;
    private int maxQuantity;
    private int desiredQuantity;

    public WarehouseQuantityRange(WarehouseKey warehouseKey, int minQuantity, int maxQuantity) {
        this.warehouseKey = warehouseKey;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
    }

    public WarehouseKey getWarehouseKey() {
        return warehouseKey;
    }

    public void setWarehouseKey(WarehouseKey warehouseKey) {
        this.warehouseKey = warehouseKey;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(int minQuantity) {
        this.minQuantity = minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(int maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public int getDesiredQuantity() {
        return desiredQuantity;
    }

    public void setDesiredQuantity(int desiredQuantity) {
        this.desiredQuantity = desiredQuantity;
    }
}
