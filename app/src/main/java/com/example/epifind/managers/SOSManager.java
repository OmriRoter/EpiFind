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

/**
 * SOSManager handles the SOS functionality within the EpiFind app.
 * It manages the activation of SOS requests, notifying nearby users,
 * and managing the user's SOS state.
 */
public class SOSManager {
    private static final long VIBRATION_DURATION = 3000; // 3 seconds
    private static final long VIBRATION_INTERVAL = 500; // 0.5 seconds

    private final Context context;
    private final Vibrator vibrator;
    private final Handler handler;
    private final UserManager userManager;
    private final DatabaseReference mDatabase;
    private final LocalNotificationManager notificationManager;
    private SOSFragment sosFragment;
    private boolean isActivating = false;

    /**
     * Interface to handle SOS activation events.
     */
    public interface SOSActivationListener {
        void onSOSActivationStarted();
        void onSOSActivationCancelled();
        void onSOSActivated();
    }

    /**
     * Constructor for SOSManager.
     *
     * @param context The context used to access system services and resources.
     */
    public SOSManager(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.userManager = UserManager.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.sosFragment = null;
        this.notificationManager = new LocalNotificationManager(context);
    }

    /**
     * Sets the associated SOSFragment, allowing interaction with the fragment's UI.
     *
     * @param fragment The SOSFragment to associate with this manager.
     */
    public void setSosFragment(SOSFragment fragment) {
        this.sosFragment = fragment;
    }

    /**
     * Starts the SOS activation process, including vibration and a delay for final activation.
     *
     * @param listener The listener to handle SOS activation events.
     */
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

    /**
     * Cancels the SOS activation process if it is still ongoing.
     *
     * @param listener The listener to handle SOS cancellation events.
     */
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

    /**
     * Activates the SOS request by obtaining the user's location and notifying nearby users.
     *
     * @param listener The listener to handle SOS activation events.
     */
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

    /**
     * Notifies nearby users with EpiPens about the active SOS request.
     *
     * @param nearbyUsers The list of nearby users to notify.
     */
    public void notifyNearbyUsers(List<UserProfile> nearbyUsers) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference sosRequestRef = mDatabase.child("sos_requests").child(currentUserId);

        sosRequestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Double requestLat = dataSnapshot.child("latitude").getValue(Double.class);
                    Double requestLon = dataSnapshot.child("longitude").getValue(Double.class);

                    for (UserProfile user : nearbyUsers) {
                        String userId = user.getUserId();
                        if (userId != null && !userId.equals(currentUserId)) {
                            float[] distance = new float[1];
                            Location.distanceBetween(requestLat, requestLon, user.getLatitude(), user.getLongitude(), distance);

                            Log.d("SOSManager", "Notifying user: " + userId + " at distance: " + distance[0] + " meters");

                            mDatabase.child("sos_notifications").child(userId).setValue(dataSnapshot.getValue());
                        }
                    }
                    mDatabase.child("latest_sos").setValue(dataSnapshot.getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SOSManager", "Failed to read SOS request data", databaseError.toException());
            }
        });
    }

    /**
     * Updates the user's SOS state in their profile.
     *
     * @param needsHelp Indicates whether the user is in need of help.
     * @param listener  The listener to handle the result of the update.
     */
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

    /**
     * Cancels the active SOS request and clears related notifications.
     *
     * @param listener The listener to handle the result of the cancellation.
     */
    private void cancelSOS(UserManager.OnUserProfileUpdateListener listener) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        mDatabase.child("sos_requests").child(userId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess();
                    notificationManager.cancelAllNotifications();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }
}
