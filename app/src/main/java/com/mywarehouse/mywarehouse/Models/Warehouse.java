package com.mywarehouse.mywarehouse.Models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Warehouse {
    private String name;
    private List<LatLng> points;
    private boolean active;

    public Warehouse() {
        // Default constructor required for calls to DataSnapshot.getValue(Warehouse.class)
    }

    public Warehouse(String name, List<LatLng> points, boolean active) {
        this.name = name;
        this.points = points;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public void setPoints(List<LatLng> points) {
        this.points = points;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
