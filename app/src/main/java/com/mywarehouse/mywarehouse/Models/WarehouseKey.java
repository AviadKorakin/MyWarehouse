package com.mywarehouse.mywarehouse.Models;

public class WarehouseKey {
    private String warehouseName;
    private ItemWarehouse itemWarehouse;

    public WarehouseKey(String warehouseName, ItemWarehouse itemWarehouse) {
        this.warehouseName = warehouseName;
        this.itemWarehouse = itemWarehouse;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public ItemWarehouse getItemWarehouse() {
        return itemWarehouse;
    }

    public void setItemWarehouse(ItemWarehouse itemWarehouse) {
        this.itemWarehouse = itemWarehouse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WarehouseKey that = (WarehouseKey) o;

        if (!warehouseName.equals(that.warehouseName)) return false;
        return itemWarehouse.equals(that.itemWarehouse);
    }

    @Override
    public int hashCode() {
        int result = warehouseName.hashCode();
        result = 31 * result + itemWarehouse.hashCode();
        return result;
    }
}
