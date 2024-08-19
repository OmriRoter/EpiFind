package com.example.epifind.models;

import com.google.firebase.auth.FirebaseAuth;

/**
 * UserProfile represents the profile of a user in the EpiFind app.
 * It contains information about the user's name, allergies, EpiPen expiry date,
 * location, and SOS response status.
 */
public class UserProfile {

    /**
     * ResponseStatus represents the availability status of the user.
     */
    public enum ResponseStatus {
        AVAILABLE,
        RESPONDING,
        UNAVAILABLE
    }

    private String userId;
    private String name = "";
    private String allergies = "";
    private String epiPenExpiry = "";
    private double latitude;
    private double longitude;
    private boolean hasEpiPen;
    private boolean needsHelp;
    private ResponseStatus responseStatus = ResponseStatus.AVAILABLE;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(UserProfile.class).
     */
    public UserProfile() {
        // Default constructor
    }

    /**
     * Constructor for creating a new UserProfile with specified details.
     *
     * @param name        The name of the user.
     * @param allergies   The allergies of the user.
     * @param epiPenExpiry The expiry date of the user's EpiPen.
     * @param latitude    The latitude of the user's location.
     * @param longitude   The longitude of the user's location.
     * @param hasEpiPen   Indicates whether the user has an EpiPen.
     */
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

    // Getters

    /**
     * Gets the user ID.
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the name of the user.
     *
     * @return The name of the user.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the allergies of the user.
     *
     * @return The allergies of the user.
     */
    public String getAllergies() {
        return allergies;
    }

    /**
     * Gets the expiry date of the user's EpiPen.
     *
     * @return The EpiPen expiry date.
     */
    public String getEpiPenExpiry() {
        return epiPenExpiry;
    }

    /**
     * Gets the latitude of the user's location.
     *
     * @return The latitude of the user's location.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Gets the longitude of the user's location.
     *
     * @return The longitude of the user's location.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Gets whether the user has an EpiPen.
     *
     * @return True if the user has an EpiPen, false otherwise.
     */
    public boolean getHasEpiPen() {
        return hasEpiPen;
    }

    /**
     * Gets whether the user needs help.
     *
     * @return True if the user needs help, false otherwise.
     */
    public boolean getNeedsHelp() {
        return needsHelp;
    }

    /**
     * Gets the response status of the user.
     *
     * @return The response status of the user.
     */
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    // Setters

    /**
     * Sets the user ID.
     *
     * @param userId The user ID.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Sets the name of the user.
     *
     * @param name The name of the user.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets whether the user needs help.
     *
     * @param needsHelp True if the user needs help, false otherwise.
     */
    public void setNeedsHelp(boolean needsHelp) {
        this.needsHelp = needsHelp;
    }

    /**
     * Sets the response status of the user.
     *
     * @param responseStatus The response status of the user.
     */
    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }
}
