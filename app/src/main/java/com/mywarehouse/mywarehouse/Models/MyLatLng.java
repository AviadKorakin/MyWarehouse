package com.mywarehouse.mywarehouse.Models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class MyLatLng implements Parcelable {
    public double latitude;
    public double longitude;

    // No-argument constructor
    public MyLatLng() {
    }

    // Parameterized constructor
    public MyLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Parcelable implementation
    protected MyLatLng(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<MyLatLng> CREATOR = new Creator<MyLatLng>() {
        @Override
        public MyLatLng createFromParcel(Parcel in) {
            return new MyLatLng(in);
        }

        @Override
        public MyLatLng[] newArray(int size) {
            return new MyLatLng[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public LatLng toLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }

    // equals method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyLatLng myLatLng = (MyLatLng) o;

        if (Double.compare(myLatLng.latitude, latitude) != 0) return false;
        return Double.compare(myLatLng.longitude, longitude) == 0;
    }

    // hashCode method
    @Override
    public int hashCode() {
        int result = Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        return result;
    }
}
