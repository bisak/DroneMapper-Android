package com.dronemapper;

public class DroneLocation {
    private double longitude;
    private double latitude;
    private double altitude;
    private double speed;
    private long timestamp;
    private double heading;
    private double distanceFromHome;

    public DroneLocation() {
    }

    public DroneLocation(double longitude, double latitude, double altitude, double speed, long time, double heading, double distanceFromHome) {
        this.longitude = Math.round(longitude * 1000000d) / 1000000d;
        this.latitude = Math.round(latitude * 1000000d) / 1000000d;
        this.altitude = Math.round(altitude * 100d) / 100d;
        this.speed = Math.round(speed * 100d) / 100d;
        this.distanceFromHome = Math.round(distanceFromHome * 100d) / 100d;
        this.timestamp = time;
        this.heading = heading;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getDistanceFromHome() {
        return distanceFromHome;
    }

    public void setDistanceFromHome(double distanceFromHome) {
        this.distanceFromHome = Math.round(distanceFromHome * 100d) / 100d;
    }

    public void setSpeed(double speed) {
        this.speed = Math.round(speed * 100d) / 100d;
    }

    public double getSpeed() {
        return speed;
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
