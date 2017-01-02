package com.dronemapper;

public class PictureData {
    public double longt;
    public double lat;
    public double alt;
    public String url;
    public String name;
    public String thumbnailUrl;
    public String description;
    public String droneTaken;
    public String dateEdited;
    public String dateTaken;
    public String dateUploaded;
    public String cameraModel;
    public String resolution;
    public String maker;

    public PictureData() {
        this.dateEdited = " - ";
        this.description = " - ";
        this.droneTaken = " - ";
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getResolution() {
        return resolution;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public String getMaker() {
        return maker;
    }

    public void setCameraModel(String cameraModel) {
        this.cameraModel = cameraModel;
    }

    public void setDateEdited(String dateEdited) {
        this.dateEdited = dateEdited;
    }

    public void setDateTaken(String dateTaken) {
        this.dateTaken = dateTaken;
    }

    public void setDateUploaded(String dateUploaded) {
        this.dateUploaded = dateUploaded;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDroneTaken(String droneTaken) {
        this.droneTaken = droneTaken;
    }

    public String getCameraModel() {
        return cameraModel;
    }

    public String getDateEdited() {
        return dateEdited;
    }

    public String getDateTaken() {
        return dateTaken;
    }

    public String getDateUploaded() {
        return dateUploaded;
    }

    public String getDescription() {
        return description;
    }

    public String getDroneTaken() {
        return droneTaken;
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
