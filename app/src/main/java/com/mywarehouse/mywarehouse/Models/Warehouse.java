package com.mywarehouse.mywarehouse.Models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    public void sortPoints() {
        if (points.size() != 4) {
            throw new IllegalArgumentException("There must be exactly 4 points.");
        }
        // Find the bottom-left point
        MyLatLng bottomLeft = Collections.min(points, new Comparator<MyLatLng>() {
            @Override
            public int compare(MyLatLng p1, MyLatLng p2) {
                if (p1.latitude != p2.latitude) {
                    return Double.compare(p1.latitude, p2.latitude);
                } else {
                    return Double.compare(p1.longitude, p2.longitude);
                }
            }
        });

        // Remove the bottom-left point from the list
        points.remove(bottomLeft);

        // Sort the remaining points based on their positions relative to the bottom-left point
        points.sort(new Comparator<MyLatLng>() {
            @Override
            public int compare(MyLatLng p1, MyLatLng p2) {
                double angle1 = Math.atan2(p1.latitude - bottomLeft.latitude, p1.longitude - bottomLeft.longitude);
                double angle2 = Math.atan2(p2.latitude - bottomLeft.latitude, p2.longitude - bottomLeft.longitude);
                return Double.compare(angle1, angle2);
            }
        });

        // Add the bottom-left point back to the beginning of the list
        points.add(0, bottomLeft);

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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warehouse warehouse = (Warehouse) o;
        return active == warehouse.active &&
                Objects.equals(name, warehouse.name) &&
                Objects.equals(points, warehouse.points);
    }
    @Override
    public int hashCode() {
        return Objects.hash(name, points, active);
    }
}
