package com.mywarehouse.mywarehouse.Models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Warehouse {
    private String name;
    private List<MyLatLng> points;
    private boolean active;

    public Warehouse() {
        // Default constructor required for calls to DataSnapshot.getValue(Warehouse.class)
    }

    public Warehouse(String name, List<LatLng> points, boolean active) {
        this.name = name;
        List<MyLatLng> newPoints=new ArrayList<>();
        for(LatLng point:points)
        {
            newPoints.add(new MyLatLng(point.latitude,point.longitude));
        }
        this.points = newPoints;
        this.active = active;
    }

    public Warehouse(List<MyLatLng> points,String name, boolean active) {
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
        List<LatLng> newPoints=new ArrayList<>();
       for(MyLatLng point:points)
       {
           newPoints.add(point.toLatLng());
       }
       return newPoints;
    }

    public void setPoints(List<MyLatLng> points) {
        this.points = points;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
