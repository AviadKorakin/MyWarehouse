package com.mywarehouse.mywarehouse.Models;

import java.util.List;

public class PickupItemWithImages {
    private PickupItem pickupItem;
    private List<String> imageUrls;

    public PickupItemWithImages(PickupItem pickupItem, List<String> imageUrls) {
        this.pickupItem = pickupItem;
        this.imageUrls = imageUrls;
    }

    public PickupItem getPickupItem() {
        return pickupItem;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PickupItemWithImages that = (PickupItemWithImages) o;

        return pickupItem.equals(that.pickupItem);
    }

    @Override
    public int hashCode() {
        return pickupItem.hashCode();
    }
}
