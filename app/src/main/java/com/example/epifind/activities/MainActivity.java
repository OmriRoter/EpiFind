package com.example.epifind.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.epifind.fragments.HomeFragment;
import com.example.epifind.models.UserProfile;
import com.example.epifind.services.LocationUpdateService;
import com.example.epifind.fragments.ProfileFragment;
import com.example.epifind.R;
import com.example.epifind.fragments.SOSFragment;
import com.example.epifind.fragments.SOSResponseFragment;
import com.example.epifind.fragments.SettingsFragment;
import com.example.epifind.managers.UserManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

/**
 * MainActivity handles the main flow of the application.
 * It manages the bottom navigation, handles permissions, and monitors SOS requests.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;
    private UserManager userManager;
    private boolean isSOSResponseShowing = false;
    private ValueEventListener sosRequestListener;
    private DatabaseReference sosRequestRef;

    // Launcher for handling notification permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                } else {
                    Log.d(TAG, "Notification permission denied");
                    Toast.makeText(this, "The app will not be able to show notifications", Toast.LENGTH_LONG).show();
                }
                requestRequiredPermissions();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userManager = UserManager.getInstance();
        setupBottomNavigation();
        checkUserProfileComplete();
        askNotificationPermission();
        setupSOSRequestListener();

        // Load the HomeFragment by default if no saved instance state exists
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sosRequestRef != null && sosRequestListener != null) {
            sosRequestRef.removeEventListener(sosRequestListener);
        }
    }

    /**
     * Sets up the bottom navigation with fragments for Home, Profile, and Settings.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    /**
     * Checks if the user profile is complete.
     * If not, navigates to the profile fragment in edit mode.
     */
    private void checkUserProfileComplete() {
        userManager.isProfileComplete(isComplete -> {
            if (!isComplete) {
                ProfileFragment profileFragment = new ProfileFragment();
                Bundle args = new Bundle();
                args.putBoolean("startInEditMode", true);
                profileFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, profileFragment)
                        .commit();
            }
        });
    }

    /**
     * Asks the user for notification permission if running on Android TIRAMISU or higher.
     * Also triggers the request for other required permissions.
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission already granted");
                requestRequiredPermissions();
            } else {
                Log.d(TAG, "Requesting notification permission");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            requestRequiredPermissions();
        }
    }

    /**
     * Requests all required permissions starting with the location permission.
     */
    private void requestRequiredPermissions() {
        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Requests a specific permission.
     * If permission is granted, handles the permission granted case based on Android version.
     *
     * @param permission The permission to request.
     */
    private void requestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting permission: " + permission);
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                onPermissionGranted(permission);
            }
        }
    }

    /**
     * Handles the case where a specific permission has been granted.
     * Requests the next required permission or starts the location service.
     *
     * @param permission The permission that was granted.
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
                break;
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                requestPermission(Manifest.permission.FOREGROUND_SERVICE);
                break;
            case Manifest.permission.FOREGROUND_SERVICE:
                requestPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
                break;
            case Manifest.permission.FOREGROUND_SERVICE_LOCATION:
                startLocationService();
                updateMapToCurrentLocation();
                break;
        }
    }

    /**
     * Updates the map to the current location if location permissions are granted.
     * If location is not available, requests a high-accuracy location update.
     */
    private void updateMapToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location == null) {
                    LocationRequest locationRequest = LocationRequest.create();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setNumUpdates(1);

                    fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            // Handle the location update if needed
                        }
                    }, null);
                }
            });
        }
    }

    /**
     * Starts the location update service to monitor the user's location in the background.
     */
    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        startService(serviceIntent);
    }

    /**
     * Checks if Google Play Services are available and shows an error dialog if not.
     */
    private void checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Objects.requireNonNull(apiAvailability.getErrorDialog(this, resultCode, 9000)).show();
            } else {
                Log.e(TAG, "This device is not supported.");
                finish();
            }
        }
    }

    /**
     * Navigates to the HomeFragment and resets the SOS response status.
     */
    public void navigateToHome() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        isSOSResponseShowing = false;
    }

    /**
     * Navigates to the SOSFragment and adds it to the back stack.
     */
    public void navigateToSOSFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SOSFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * Sets up a listener for SOS requests from the Firebase Realtime Database.
     * If an active SOS request is received, it triggers the appropriate actions.
     */
    private void setupSOSRequestListener() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        sosRequestRef = FirebaseDatabase.getInstance().getReference("sos_notifications").child(userId);
        sosRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !isSOSResponseShowing) {
                    String requesterId = dataSnapshot.child("requester").getValue(String.class);
                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);
                    Boolean active = dataSnapshot.child("active").getValue(Boolean.class);

                    if (requesterId != null && !requesterId.equals(userId) && latitude != null && longitude != null && active != null && active) {
                        // Get current user's location
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                                if (location != null) {
                                    float[] distance = new float[1];
                                    Location.distanceBetween(latitude, longitude,
                                            location.getLatitude(), location.getLongitude(), distance);

                                    Log.d("MainActivity", "Received SOS notification. Distance: " + distance[0] + " meters");

                                    showSOSNotification();
                                    showSOSResponseFragment(requesterId, latitude, longitude);
                                    isSOSResponseShowing = true;
                                } else {
                                    Log.e("MainActivity", "Unable to get current location");
                                }
                            });
                        } else {
                            Log.e("MainActivity", "Location permission not granted");
                        }
                    } else {
                        Log.e(TAG, "Incomplete or inactive SOS data received");
                    }
                } else if (!dataSnapshot.exists()) {
                    isSOSResponseShowing = false;
                    resetUserStatus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "SOS request listener cancelled", databaseError.toException());
            }
        };
        sosRequestRef.addValueEventListener(sosRequestListener);
    }

    /**
     * Resets the user's status to AVAILABLE in the Firebase Realtime Database.
     */
    private void resetUserStatus() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.child("responseStatus").setValue(UserProfile.ResponseStatus.AVAILABLE.name())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to reset user status", e));
    }

    /**
     * Displays the SOS response fragment to the user.
     *
     * @param requesterId The ID of the user who sent the SOS request.
     * @param latitude    The latitude of the SOS location.
     * @param longitude   The longitude of the SOS location.
     */
    private void showSOSResponseFragment(String requesterId, double latitude, double longitude) {
        SOSResponseFragment fragment = new SOSResponseFragment();
        Bundle args = new Bundle();
        args.putString("requesterId", requesterId);
        args.putDouble("latitude", latitude);
        args.putDouble("longitude", longitude);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Displays a notification to the user about an SOS request.
     */
    public void showSOSNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SOS_CHANNEL")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SOS Request")
                .setContentText("Someone needs help!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent;
        pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(1, builder.build());
            } else {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            notificationManager.notify(1, builder.build());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSOSNotification();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
