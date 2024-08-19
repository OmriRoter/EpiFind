package com.example.epifind.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.epifind.R;
import com.example.epifind.managers.SOSManager;
import com.example.epifind.managers.UserManager;
import com.example.epifind.activities.MainActivity;
import com.example.epifind.utils.EpiPenExpiryChecker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.example.epifind.databinding.FragmentHomeBinding;

/**
 * HomeFragment is the main screen of the app that shows a map and handles SOS activation.
 * It also checks for EpiPen expiration and alerts the user when necessary.
 */
public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private SOSManager sosManager;
    private UserManager userManager;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        initializeComponents();
        setupViews();
        return binding.getRoot();
    }

    /**
     * Initializes the necessary components such as managers and location services.
     */
    private void initializeComponents() {
        userManager = UserManager.getInstance();
        sosManager = new SOSManager(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    /**
     * Sets up the views, including the map, SOS button, and EpiPen expiry checks.
     */
    private void setupViews() {
        initializeMap();
        setupSosButton();
        setupDismissButton();
        checkEpiPenExpiry();
    }

    /**
     * Initializes the map fragment and sets up the callback for when the map is ready.
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Sets up the dismiss button to hide the alert container when clicked.
     */
    private void setupDismissButton() {
        binding.dismissButton.setOnClickListener(v -> binding.alertContainer.setVisibility(View.GONE));
    }

    @Override
    public void onResume() {
        super.onResume();
        checkEpiPenExpiry();
    }

    /**
     * Checks if the user's EpiPen is close to expiration and shows an alert if necessary.
     */
    private void checkEpiPenExpiry() {
        EpiPenExpiryChecker.checkEpiPenExpiry(userManager, this::handleEpiPenExpiry);
    }

    /**
     * Handles the EpiPen expiry alert logic based on the number of days until expiry.
     *
     * @param daysUntilExpiry Number of days until the EpiPen expires.
     */
    private void handleEpiPenExpiry(int daysUntilExpiry) {
        Context context = getContext();
        if (context != null) {
            if (daysUntilExpiry <= 7 && daysUntilExpiry > 0) {
                showExpiryAlert(daysUntilExpiry);
            } else if (daysUntilExpiry == 0) {
                showExpiryAlert(daysUntilExpiry);
                Toast.makeText(context, "Your EpiPen expires today! Please replace it.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Shows an alert to the user regarding the EpiPen expiry status.
     *
     * @param days Number of days until the EpiPen expires.
     */
    @SuppressLint("SetTextI18n")
    private void showExpiryAlert(int days) {
        binding.alertMessage.setText(days == 0
                ? "Your EpiPen has expired! Please replace it immediately."
                : "Your EpiPen will expire in " + days + " days!");
        binding.alertContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Sets up the SOS button with touch listeners to handle SOS activation and cancellation.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupSosButton() {
        binding.sosButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sosManager.startSOSActivation(new SOSManager.SOSActivationListener() {
                        @Override
                        public void onSOSActivationStarted() {
                            // You can add any UI changes here, like changing the button color
                        }

                        @Override
                        public void onSOSActivationCancelled() {
                            Toast.makeText(requireContext(), "SOS Cancelled", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onSOSActivated() {
                            activateSOS();
                        }
                    });
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sosManager.cancelSOSActivation(new SOSManager.SOSActivationListener() {
                        @Override
                        public void onSOSActivationStarted() {}
                        @Override
                        public void onSOSActivationCancelled() {}
                        @Override
                        public void onSOSActivated() {}
                    });
                    return true;
            }
            return false;
        });
    }

    /**
     * Activates the SOS mode and updates the user's profile with the SOS status.
     */
    private void activateSOS() {
        sosManager.updateSOSState(true, new UserManager.OnUserProfileUpdateListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "SOS Activated", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSOSFragment();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "Failed to activate SOS: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Enables the My Location layer on the map, allowing the app to display the user's current location.
     */
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            zoomToCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Zooms the map to the user's current location if location permissions are granted.
     */
    private void zoomToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null && mMap != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission is required for this feature", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }
}
