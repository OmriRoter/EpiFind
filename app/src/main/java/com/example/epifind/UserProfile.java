package com.example.epifind;

public class UserProfile {
    private String name;
    private String allergies;
    private String epiPenExpiry;

    // Default constructor required for calls to DataSnapshot.getValue(UserProfile.class)
    public UserProfile() {}

    public UserProfile(String name, String allergies, String epiPenExpiry) {
        this.name = name;
        this.allergies = allergies;
        this.epiPenExpiry = epiPenExpiry;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getEpiPenExpiry() { return epiPenExpiry; }
    public void setEpiPenExpiry(String epiPenExpiry) { this.epiPenExpiry = epiPenExpiry; }
}