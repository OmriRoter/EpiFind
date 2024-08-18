package com.example.epifind.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class SOSRequest {
    private String id;
    private String requesterId;
    private List<String> nearbyUserIds;
    private RequestStatus status;
    private long timestamp;
    private double latitude;
    private double longitude;
    private Map<String, ResponseStatus> respondingUsers;

    public enum RequestStatus {
        ACTIVE, CANCELLED, HANDLED
    }

    public enum ResponseStatus {
        RESPONDING, UNAVAILABLE
    }

    public SOSRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(SOSRequest.class)
    }

    public SOSRequest(String id, String requesterId, double latitude, double longitude) {
        this.id = id;
        this.requesterId = requesterId;
        this.nearbyUserIds = new ArrayList<>();
        this.status = RequestStatus.ACTIVE;
        this.timestamp = System.currentTimeMillis();
        this.latitude = latitude;
        this.longitude = longitude;
        this.respondingUsers = new HashMap<>();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public List<String> getNearbyUserIds() {
        return nearbyUserIds;
    }

    public void setNearbyUserIds(List<String> nearbyUserIds) {
        this.nearbyUserIds = nearbyUserIds;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public Map<String, ResponseStatus> getRespondingUsers() {
        return respondingUsers;
    }

    public void setRespondingUsers(Map<String, ResponseStatus> respondingUsers) {
        this.respondingUsers = respondingUsers;
    }

    // Helper methods

    public void addNearbyUser(String userId) {
        if (!nearbyUserIds.contains(userId)) {
            nearbyUserIds.add(userId);
        }
    }

    public void removeNearbyUser(String userId) {
        nearbyUserIds.remove(userId);
    }

    public void addRespondingUser(String userId, ResponseStatus status) {
        respondingUsers.put(userId, status);
    }

    public void removeRespondingUser(String userId) {
        respondingUsers.remove(userId);
    }

    @Exclude
    public boolean isStillRelevant(long currentTime, long maxDuration) {
        return (currentTime - timestamp) <= maxDuration;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("requesterId", requesterId);
        result.put("nearbyUserIds", nearbyUserIds);
        result.put("status", status.name());
        result.put("timestamp", timestamp);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("respondingUsers", respondingUsers);
        return result;
    }
}
