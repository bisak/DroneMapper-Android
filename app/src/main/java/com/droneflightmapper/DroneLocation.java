package com.droneflightmapper;

/**
 * Created by biskazz on 21.9.2016 г..
 */

public class DroneLocation {
    public double longitude;
    public double latitude;
    public double altitude;
    public double speed;


    public DroneLocation() {
    }

    public DroneLocation(double longitude, double latitude, double altitude, double speed) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.speed = speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getSpeed() {
        return speed;
    }


    public double getAltitude() {
        return altitude;
    }


    public void setAltitude(double altitude) {
        this.altitude = altitude;
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
