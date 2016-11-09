package com.dronemapper;

public class PictureData {
    public double longt;
    public double lat;
    public double alt;
    public String url;
    public String name;
    public String thumbnailUrl;

    public PictureData() {
    }

    public PictureData(double longt, double lat, double alt, String name, String url, String thumbnailUrl) {
        this.longt = longt;
        this.lat = lat;
        this.alt = alt;
        this.url = url;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
    }

    public double getAlt() {
        return alt;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLongt() {
        return longt;
    }

    public void setLongt(double longt) {
        this.longt = longt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
