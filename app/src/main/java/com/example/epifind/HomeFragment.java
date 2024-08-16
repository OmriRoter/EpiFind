package com.example.epifind;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.epifind.databinding.FragmentHomeBinding;


public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private SOSManager sosManager;
    private boolean isSOSButtonPressed = false;


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        setupSosButton();

        sosManager = new SOSManager(requireContext());
        setupSosButton();

        return view;
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