package com.example.epifind.managers;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.epifind.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * UserManager is responsible for managing user profiles in the EpiFind app.
 * It provides methods to create, update, and retrieve user profiles, as well as to check profile completeness.
 */
public class UserManager {
    private static final String TAG = "UserManager";
    private static UserManager instance;
    private final DatabaseReference mDatabase;
    private final FirebaseAuth mAuth;

    /**
     * Interface for handling user profile update results.
     */
    public interface OnUserProfileUpdateListener {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Interface for handling user profile fetch results.
     */
    public interface OnUserProfileFetchListener {
        void onSuccess(UserProfile userProfile);
        void onFailure(String error);
    }

    /**
     * Interface for checking whether a user's profile is complete.
     */
    public interface OnProfileCheckListener {
        void onResult(boolean isComplete);
    }

    /**
     * Private constructor for UserManager to implement Singleton pattern.
     */
    private UserManager() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Retrieves the single instance of UserManager.
     *
     * @return The single instance of UserManager.
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Creates or updates a user profile in the Firebase Realtime Database.
     *
     * @param userProfile The user profile to create or update.
     * @param listener    The listener to handle the result of the operation.
     */
    public void createOrUpdateUser(UserProfile userProfile, final OnUserProfileUpdateListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            handleNoUserSignedIn(listener);
            return;
        }

        String userId = currentUser.getUid();
        userProfile.setUserId(userId);

        mDatabase.child("users").child(userId).setValue(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated successfully");
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Retrieves the current user's profile from Firebase Realtime Database.
     *
     * @param listener The listener to handle the result of the fetch operation.
     */
    public void getUserProfile(final OnUserProfileFetchListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            handleNoUserSignedIn(listener);
            return;
        }

        String userId = currentUser.getUid();
        fetchUserProfile(userId, listener);
    }

    /**
     * Retrieves a user profile by user ID from Firebase Realtime Database.
     *
     * @param userId   The ID of the user whose profile to fetch.
     * @param listener The listener to handle the result of the fetch operation.
     */
    public void getUserProfileById(String userId, final OnUserProfileFetchListener listener) {
        fetchUserProfile(userId, listener);
    }

    /**
     * Fetches a user profile from Firebase Realtime Database.
     *
     * @param userId   The ID of the user whose profile to fetch.
     * @param listener The listener to handle the result of the fetch operation.
     */
    private void fetchUserProfile(String userId, final OnUserProfileFetchListener listener) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                if (userProfile != null) {
                    userProfile.setUserId(dataSnapshot.getKey());
                    Log.d(TAG, "User profile fetched successfully");
                    if (listener != null) {
                        listener.onSuccess(userProfile);
                    }
                } else {
                    Log.d(TAG, "User profile does not exist");
                    if (listener != null) {
                        listener.onFailure("User profile does not exist");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user profile", databaseError.toException());
                if (listener != null) {
                    listener.onFailure(databaseError.getMessage());
                }
            }
        });
    }

    /**
     * Checks if the current user's profile is complete.
     *
     * @param listener The listener to handle the result of the check operation.
     */
    public void isProfileComplete(final OnProfileCheckListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            handleNoUserSignedIn(listener);
            return;
        }

        String userId = currentUser.getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null) {
                        boolean isComplete = !TextUtils.isEmpty(userProfile.getName()) &&
                                !TextUtils.isEmpty(userProfile.getAllergies()) &&
                                !TextUtils.isEmpty(userProfile.getEpiPenExpiry());
                        listener.onResult(isComplete);
                    } else {
                        Log.d(TAG, "UserProfile is null");
                        listener.onResult(false);
                    }
                } else {
                    Log.d(TAG, "User data does not exist");
                    listener.onResult(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking profile completeness", databaseError.toException());
                listener.onResult(false);
            }
        });
    }

    /**
     * Updates the response status of the current user in Firebase Realtime Database.
     *
     * @param status   The response status to set.
     * @param listener The listener to handle the result of the update operation.
     */
    public void updateUserResponseStatus(UserProfile.ResponseStatus status, final OnUserProfileUpdateListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            handleNoUserSignedIn(listener);
            return;
        }

        String userId = currentUser.getUid();
        mDatabase.child("users").child(userId).child("responseStatus").setValue(status.name())
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Handles the case where no user is signed in, triggering a failure callback.
     *
     * @param listener The listener to notify of the failure.
     */
    private void handleNoUserSignedIn(OnUserProfileUpdateListener listener) {
        Log.e(TAG, "No user is signed in");
        if (listener != null) {
            listener.onFailure("No user is signed in");
        }
    }

    /**
     * Handles the case where no user is signed in, triggering a failure callback.
     *
     * @param listener The listener to notify of the failure.
     */
    private void handleNoUserSignedIn(OnUserProfileFetchListener listener) {
        Log.e(TAG, "No user is signed in");
        if (listener != null) {
            listener.onFailure("No user is signed in");
        }
    }

    /**
     * Handles the case where no user is signed in, triggering a failure callback.
     *
     * @param listener The listener to notify of the failure.
     */
    private void handleNoUserSignedIn(OnProfileCheckListener listener) {
        Log.e(TAG, "No user is signed in");
        if (listener != null) {
            listener.onResult(false);
        }
    }
}
