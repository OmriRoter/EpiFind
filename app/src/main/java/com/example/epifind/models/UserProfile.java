package com.example.epifind.models;

import com.google.firebase.auth.FirebaseAuth;

public class UserProfile {
    private String name = "";
    private String allergies = "";
    private String epiPenExpiry = "";
    private double latitude;
    private double longitude;
    private boolean hasEpiPen;
    private boolean needsHelp;
    private boolean sosRequested = false;
    private String userId;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public enum ResponseStatus {
        AVAILABLE,
        RESPONDING,
        UNAVAILABLE
    }

    public UserProfile() {
    }

    public UserProfile(String name, String allergies, String epiPenExpiry, double latitude, double longitude, boolean hasEpiPen) {
        this.name = name != null ? name : "";
        this.allergies = allergies != null ? allergies : "";
        this.epiPenExpiry = epiPenExpiry != null ? epiPenExpiry : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasEpiPen = hasEpiPen;
        this.userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    private ResponseStatus responseStatus = ResponseStatus.AVAILABLE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getEpiPenExpiry() {
        return epiPenExpiry;
    }

    public void setEpiPenExpiry(String epiPenExpiry) {
        this.epiPenExpiry = epiPenExpiry;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean getHasEpiPen() {
        return hasEpiPen;
    }

    public void setHasEpiPen(boolean hasEpiPen) {
        this.hasEpiPen = hasEpiPen;
    }

    public boolean getNeedsHelp() {
        return needsHelp;
    }

    public void setNeedsHelp(boolean needsHelp) {
        this.needsHelp = needsHelp;
    }

    public boolean isSosRequested() {
        return sosRequested;
    }

    public void setSosRequested(boolean sosRequested) {
        this.sosRequested = sosRequested;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

}
