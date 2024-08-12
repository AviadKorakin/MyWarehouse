package com.mywarehouse.mywarehouse.Models;

public class SelectedItemWarehouse {
    private ItemWarehouse itemWarehouse;
    private int fulfilledQuantity;

    public SelectedItemWarehouse(ItemWarehouse itemWarehouse, int fulfilledQuantity) {
        this.itemWarehouse = itemWarehouse;
        this.fulfilledQuantity = fulfilledQuantity;
    }

    public ItemWarehouse getItemWarehouse() {
        return itemWarehouse;
    }

    public int getFulfilledQuantity() {
        return fulfilledQuantity;
    }
}