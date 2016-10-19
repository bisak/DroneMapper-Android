package com.droneflightmapper;

public class DroneLocation {
    public double longitude;
    public double latitude;
    public double altitude;
    public double speed;
    public long time;
    public double heading;

    public DroneLocation() {
    }

    public DroneLocation(double longitude, double latitude, double altitude, double speed, long time, double heading) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.speed = speed;
        this.time = time;
        this.heading = heading;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }


    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getSpeed() {
        return speed;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
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
