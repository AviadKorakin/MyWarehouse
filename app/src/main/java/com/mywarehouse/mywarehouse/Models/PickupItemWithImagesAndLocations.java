package com.mywarehouse.mywarehouse.Models;

import java.util.List;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(pickupItemWithImages, locations);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PickupItemWithImagesAndLocations that = (PickupItemWithImagesAndLocations) o;
        return Objects.equals(pickupItemWithImages, that.pickupItemWithImages) &&
                Objects.equals(locations, that.locations);
    }
}
