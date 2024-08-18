package com.example.epifind.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;
    private UserManager userManager;
    private boolean isSOSResponseShowing = false;
    private ValueEventListener sosRequestListener;
    private DatabaseReference sosRequestRef;

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

    private void requestRequiredPermissions() {
        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

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

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        startService(serviceIntent);
    }

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

    public void navigateToHome() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        isSOSResponseShowing = false;
    }

    public void navigateToSOSFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SOSFragment())
                .addToBackStack(null)
                .commit();
    }

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
                        showSOSNotification();
                        showSOSResponseFragment(requesterId, latitude, longitude);
                        isSOSResponseShowing = true;
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

    private void resetUserStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.child("responseStatus").setValue(UserProfile.ResponseStatus.AVAILABLE.name())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to reset user status", e));
    }


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

    private void showSOSNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SOS_CHANNEL")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("SOS Request")
                .setContentText("Someone needs help!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

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