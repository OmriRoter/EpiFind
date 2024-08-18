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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
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

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private SOSManager sosManager;
    private UserManager userManager;
    private FrameLayout alertContainer;
    private TextView alertMessage;
    private Button dismissButton;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        userManager = UserManager.getInstance();
        sosManager = new SOSManager(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize alert views
        alertContainer = view.findViewById(R.id.alertContainer);
        alertMessage = view.findViewById(R.id.alertMessage);
        dismissButton = view.findViewById(R.id.dismissButton);

        // Set up SOS button
        setupSosButton();

        // Set up dismiss button for alert
        dismissButton.setOnClickListener(v -> alertContainer.setVisibility(View.GONE));

        // Check EpiPen expiry
        checkEpiPenExpiry();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkEpiPenExpiry();
    }

    private void checkEpiPenExpiry() {
        EpiPenExpiryChecker.checkEpiPenExpiry(userManager, daysUntilExpiry -> {
            if (daysUntilExpiry <= 7 && daysUntilExpiry > 0) {
                Context context = getContext();
                if (context != null) {
                    showExpiryAlert(daysUntilExpiry);
                }
            } else if (daysUntilExpiry == 0) {
                Context context = getContext();
                if (context != null) {
                    showExpiryAlert(daysUntilExpiry);
                    Toast.makeText(context, "Your EpiPen expires today! Please replace it.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void showExpiryAlert(int days) {
        if (days == 0) {
            alertMessage.setText("Your EpiPen has expired! Please replace it immediately.");
        } else {
            alertMessage.setText("Your EpiPen will expire in " + days + " days!");
        }
        alertContainer.setVisibility(View.VISIBLE);
    }

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
                            // Reset any UI changes made during activation
                            Toast.makeText(requireContext(), "SOS Cancelled", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onSOSActivated() {
                            // Update SOS state in UserProfile and Firebase
                            sosManager.updateSOSState(true, new UserManager.OnUserProfileUpdateListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(requireContext(), "SOS Activated", Toast.LENGTH_SHORT).show();
                                    // Navigate to SOS screen
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
                    });
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sosManager.cancelSOSActivation(new SOSManager.SOSActivationListener() {
                        @Override
                        public void onSOSActivationStarted() {
                        }

                        @Override
                        public void onSOSActivationCancelled() {
                        }

                        @Override
                        public void onSOSActivated() {
                        }
                    });
                    return true;
            }
            return false;
        });
    }

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