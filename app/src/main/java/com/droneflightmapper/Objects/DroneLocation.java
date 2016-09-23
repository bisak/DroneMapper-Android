package com.droneflightmapper;

/**
 * Created by biskazz on 21.9.2016 г..
 */

public class DroneLocation {
    public double longitude;
    public double latitude;

    public DroneLocation() {
    }

    public DroneLocation(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
