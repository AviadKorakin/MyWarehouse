package com.mywarehouse.mywarehouse.Models;

import java.util.List;

public class PickupItemWithImagesAndLocations {
    private PickupItemWithImages pickupItemWithImages;
    private List<ItemWarehouse> locations;

    public PickupItemWithImagesAndLocations(PickupItemWithImages pickupItemWithImages, List<ItemWarehouse> locations) {
        this.pickupItemWithImages = pickupItemWithImages;
        this.locations = locations;
    }

    public PickupItemWithImages getPickupItemWithImages() {
        return pickupItemWithImages;
    }

    public List<ItemWarehouse> getLocations() {
        return locations;
    }
}
