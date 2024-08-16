package com.example.epifind;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserManager {
    private static final String TAG = "UserManager";
    private static UserManager instance;
    private final DatabaseReference mDatabase;
    private final FirebaseAuth mAuth;

    private UserManager() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void createOrUpdateUser(UserProfile userProfile, final OnUserProfileUpdateListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is signed in");
            if (listener != null) {
                listener.onFailure("No user is signed in");
            }
            return;
        }

        String userId = currentUser.getUid();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile existingProfile = dataSnapshot.getValue(UserProfile.class);
                if (existingProfile != null) {
                    // שמירה על ה-FCM token הקיים
                    userProfile.setFcmToken(existingProfile.getFcmToken());
                    // שמירה על נתוני מיקום קיימים
                    userProfile.setLatitude(existingProfile.getLatitude());
                    userProfile.setLongitude(existingProfile.getLongitude());
                }

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

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching existing user profile", databaseError.toException());
                if (listener != null) {
                    listener.onFailure(databaseError.getMessage());
                }
            }
        });
    }

    public void updateFCMToken(String token, OnUserProfileUpdateListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is signed in");
            if (listener != null) {
                listener.onFailure("No user is signed in");
            }
            return;
        }

        String userId = currentUser.getUid();
        mDatabase.child("users").child(userId).child("fcmToken").setValue(token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token updated successfully");
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating FCM token", e);
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    public void getUserProfile(final OnUserProfileFetchListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is signed in");
            if (listener != null) {
                listener.onFailure("No user is signed in");
            }
            return;
        }

        String userId = currentUser.getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                if (userProfile != null) {
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

    public void isProfileComplete(final OnProfileCheckListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user is signed in");
            if (listener != null) {
                listener.onResult(false);
            }
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

    public interface OnUserProfileUpdateListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnUserProfileFetchListener {
        void onSuccess(UserProfile userProfile);
        void onFailure(String error);
    }

    public interface OnProfileCheckListener {
        void onResult(boolean isComplete);
    }
}