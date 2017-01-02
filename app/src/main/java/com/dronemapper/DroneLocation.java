package com.dronemapper;

public class DroneLocation {
    private double longitude;
    private double latitude;
    private double altitude;
    private double horizSpeed;
    private double vertSpeed;
    private long timestamp;
    private int heading;
    private double distanceFromHome;

    public DroneLocation() {
        this.longitude = 0.404;
        this.latitude = 0.404;
        this.altitude = 0.404;
        this.horizSpeed = 0.404;
        this.vertSpeed = 0.404;
        this.distanceFromHome = 0.404;
        this.timestamp = 404;
        this.heading = 404;
    }

    public DroneLocation(double longitude, double latitude, double altitude, double horizSpeed, double vertSpeed, long time, int heading, double distanceFromHome) {
        this.longitude = Math.round(longitude * 1000000d) / 1000000d;
        this.latitude = Math.round(latitude * 1000000d) / 1000000d;
        this.altitude = Math.round(altitude * 100d) / 100d;
        this.horizSpeed = Math.round(horizSpeed * 100d) / 100d;
        this.vertSpeed = Math.round(vertSpeed * 100d) / 100d;
        this.distanceFromHome = Math.round(distanceFromHome * 100d) / 100d;
        this.timestamp = time;
        this.heading = heading;
    }

    public int getHeading() {
        return heading;
    }

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public double getDistanceFromHome() {
        return distanceFromHome;
    }

    public void setDistanceFromHome(double distanceFromHome) {
        this.distanceFromHome = Math.round(distanceFromHome * 100d) / 100d;
    }

    public void setHorizSpeed(double horizSpeed) {
        this.horizSpeed = Math.round(horizSpeed * 100d) / 100d;
    }

    public double getHorizSpeed() {
        return horizSpeed;
    }

    public void setVertSpeed(double vertSpeed) {
        this.vertSpeed = Math.round(vertSpeed * 100d) / 100d;
    }

    public double getVertSpeed() {
        return vertSpeed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = Math.round(altitude * 100d) / 100d;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = Math.round(longitude * 1000000d) / 1000000d;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = Math.round(latitude * 1000000d) / 1000000d;
    }
}
