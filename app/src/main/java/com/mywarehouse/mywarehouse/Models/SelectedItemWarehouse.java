package com.mywarehouse.mywarehouse.Models;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectedItemWarehouse that = (SelectedItemWarehouse) o;
        return fulfilledQuantity == that.fulfilledQuantity &&
                Objects.equals(itemWarehouse, that.itemWarehouse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemWarehouse, fulfilledQuantity);
    }
}