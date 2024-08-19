package com.example.epifind.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.epifind.R;
import com.example.epifind.activities.MainActivity;
import com.example.epifind.managers.LocalNotificationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * LocationUpdateService is a foreground service that continuously tracks the user's location and updates it in Firebase.
 * It also listens for SOS alerts from nearby users and notifies the user if someone needs help.
 */
public class LocationUpdateService extends Service {

    private static final String TAG = "LocationUpdateService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 12345;
    private static final float MIN_DISTANCE_FOR_UPDATE = 10; // 10 meters

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private Location lastLocation;
    private DatabaseReference latestSosRef;
    private ValueEventListener sosListener;
    private LocalNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeComponents();
        setupLocationCallback();
        setupSosListener();
    }

    /**
     * Initializes the components needed for the service, such as location client, Firebase references, and notification manager.
     */
    private void initializeComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        notificationManager = new LocalNotificationManager(this);
        latestSosRef = FirebaseDatabase.getInstance().getReference("latest_sos");
    }

    /**
     * Sets up the location callback to handle location updates.
     * The callback checks if the location should be updated in Firebase based on the distance moved.
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (shouldUpdateLocation(location)) {
                        updateLocationInFirebase(location);
                        lastLocation = location;
                    }
                }
            }
        };
    }

    /**
     * Sets up a listener to monitor SOS alerts from other users.
     * If an SOS alert is detected and the user is not the requester, a notification is shown.
     */
    private void setupSosListener() {
        sosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String requesterId = dataSnapshot.child("requester").getValue(String.class);
                    if (requesterId != null && !requesterId.equals(currentUser.getUid())) {
                        notificationManager.showNotification(
                                "SOS Alert",
                                "Someone nearby needs help with an EpiPen!"
                        );
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read latest SOS data", databaseError.toException());
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && checkPermissions()) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification());
            requestLocationUpdates();
        }
        latestSosRef.addValueEventListener(sosListener);
        return START_STICKY;
    }

    /**
     * Checks whether the necessary permissions have been granted.
     *
     * @return True if all required permissions are granted, false otherwise.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Creates a notification channel for the service, required for Android O and above.
     */
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    /**
     * Builds the notification that will be shown when the service is running in the foreground.
     *
     * @return The notification to be displayed.
     */
    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("EpiFind is active")
                .setContentText("Tracking your location for emergency assistance")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    /**
     * Requests location updates from the FusedLocationProviderClient.
     * Updates are requested at a high accuracy and frequent interval.
     */
    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (checkPermissions()) {
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest,
                            locationCallback,
                            Looper.getMainLooper());
                } catch (SecurityException e) {
                    Log.e(TAG, "Error requesting location updates", e);
                }
            }
        }
    }

    /**
     * Determines whether the new location should be updated in Firebase based on the distance moved.
     *
     * @param newLocation The new location to evaluate.
     * @return True if the location should be updated, false otherwise.
     */
    private boolean shouldUpdateLocation(Location newLocation) {
        return lastLocation == null || newLocation.distanceTo(lastLocation) >= MIN_DISTANCE_FOR_UPDATE;
    }

    /**
     * Updates the user's location in Firebase Realtime Database.
     *
     * @param location The location to update.
     */
    private void updateLocationInFirebase(Location location) {
        if (currentUser != null) {
            mDatabase.child("users").child(currentUser.getUid()).child("latitude").setValue(location.getLatitude());
            mDatabase.child("users").child(currentUser.getUid()).child("longitude").setValue(location.getLongitude());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (sosListener != null) {
            latestSosRef.removeEventListener(sosListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
