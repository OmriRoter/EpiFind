package com.example.epifind.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.epifind.fragments.SOSFragment;
import com.example.epifind.models.UserProfile;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SOSManager {
    private static final String TAG = "SOSManager";
    private final Context context;
    private final Vibrator vibrator;
    private final Handler handler;
    private static final long VIBRATION_DURATION = 3000; // 3 seconds
    private static final long VIBRATION_INTERVAL = 500; // 0.5 seconds
    private boolean isActivating = false;
    private final UserManager userManager;
    private final DatabaseReference mDatabase;
    private SOSFragment sosFragment;
    private final LocalNotificationManager notificationManager;

    public interface SOSActivationListener {
        void onSOSActivationStarted();
        void onSOSActivationCancelled();
        void onSOSActivated();
    }

    public SOSManager(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.userManager = UserManager.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.sosFragment = null;
        this.notificationManager = new LocalNotificationManager(context);
    }

    public void setSosFragment(SOSFragment fragment) {
        this.sosFragment = fragment;
    }

    public void startSOSActivation(SOSActivationListener listener) {
        isActivating = true;
        listener.onSOSActivationStarted();

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, VIBRATION_INTERVAL, VIBRATION_INTERVAL};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        }

        handler.postDelayed(() -> {
            if (isActivating) {
                if (vibrator != null) {
                    vibrator.cancel();
                }
                activateSOS(listener);
                isActivating = false;
            }
        }, VIBRATION_DURATION);
    }

    public void cancelSOSActivation(SOSActivationListener listener) {
        if (isActivating) {
            handler.removeCallbacksAndMessages(null);
            if (vibrator != null) {
                vibrator.cancel();
            }
            listener.onSOSActivationCancelled();
            isActivating = false;
        }
    }

    public void activateSOS(SOSActivationListener listener) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    Map<String, Object> sosData = new HashMap<>();
                    sosData.put("requester", userId);
                    sosData.put("latitude", location.getLatitude());
                    sosData.put("longitude", location.getLongitude());
                    sosData.put("timestamp", ServerValue.TIMESTAMP);
                    sosData.put("active", true);

                    // Clear existing responses
                    mDatabase.child("sos_responses").child(userId).removeValue();

                    mDatabase.child("sos_requests").child(userId).setValue(sosData)
                            .addOnSuccessListener(aVoid -> updateSOSState(true, new UserManager.OnUserProfileUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    listener.onSOSActivated();
                                    if (sosFragment != null) {
                                        sosFragment.findNearbyUsersWithEpiPen();
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    listener.onSOSActivationCancelled();
                                }
                            }))
                            .addOnFailureListener(e -> listener.onSOSActivationCancelled());
                } else {
                    listener.onSOSActivationCancelled();
                }
            });
        } else {
            listener.onSOSActivationCancelled();
        }
    }

    public void notifyNearbyUsers(List<UserProfile> nearbyUsers) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference sosRequestRef = mDatabase.child("sos_requests").child(currentUserId);

        sosRequestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (UserProfile user : nearbyUsers) {
                        String userId = user.getUserId();
                        if (userId != null && !userId.equals(currentUserId)) {
                            mDatabase.child("sos_notifications").child(userId).setValue(dataSnapshot.getValue());
                        }
                    }
                    // Update a general SOS node to trigger notifications
                    mDatabase.child("latest_sos").setValue(dataSnapshot.getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read SOS request data", databaseError.toException());
            }
        });
    }

    public void updateSOSState(boolean needsHelp, final UserManager.OnUserProfileUpdateListener listener) {
        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                userProfile.setNeedsHelp(needsHelp);
                userManager.createOrUpdateUser(userProfile, new UserManager.OnUserProfileUpdateListener() {
                    @Override
                    public void onSuccess() {
                        if (!needsHelp) {
                            cancelSOS(listener);
                        } else {
                            listener.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        listener.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                listener.onFailure("Failed to fetch user profile: " + error);
            }
        });
    }

    private void cancelSOS(UserManager.OnUserProfileUpdateListener listener) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        mDatabase.child("sos_requests").child(userId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess();
                    notificationManager.cancelAllNotifications();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deleteSosRequest(String requesterId, OnSosRequestDeleteListener listener) {
        DatabaseReference sosRequestsRef = FirebaseDatabase.getInstance().getReference("sos_requests");
        sosRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean requestFound = false;
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String requester = childSnapshot.child("requester").getValue(String.class);
                    if (requester != null && requester.equals(requesterId)) {
                        childSnapshot.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "SOS request deleted successfully");
                                    if (listener != null) {
                                        listener.onSuccess();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting SOS request", e);
                                    if (listener != null) {
                                        listener.onFailure(e.getMessage());
                                    }
                                });
                        requestFound = true;
                        break;
                    }
                }
                if (!requestFound && listener != null) {
                    listener.onFailure("SOS request not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error searching for SOS request", databaseError.toException());
                if (listener != null) {
                    listener.onFailure(databaseError.getMessage());
                }
            }
        });
    }

    public interface OnSosRequestDeleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}