package com.example.epifind;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SOSFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap mMap;
    private RecyclerView usersRecyclerView;
    private UserAdapter userAdapter;
    private List<UserProfile> nearbyUsersWithEpiPen = new ArrayList<>();
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private static final float SEARCH_RADIUS = 2000; // 2 km in meters
    private Location currentLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private SOSManager sosManager;
    private TextView usersCountTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sos_fragment, container, false);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        usersRecyclerView = view.findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(nearbyUsersWithEpiPen);
        usersRecyclerView.setAdapter(userAdapter);

        usersCountTextView = view.findViewById(R.id.usersCountTextView);

        sosManager = new SOSManager(requireContext());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        view.findViewById(R.id.cancelSOSButton).setOnClickListener(v -> cancelSOS());

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
    }

    private void cancelSOS() {
        sosManager.updateSOSState(false, new UserManager.OnUserProfileUpdateListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "SOS Cancelled", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToHome();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Failed to cancel SOS: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        findNearbyUsersWithEpiPen();
                    } else {
                        Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void findNearbyUsersWithEpiPen() {
        if (currentLocation == null) return;

        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nearbyUsersWithEpiPen.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    UserProfile user = userSnapshot.getValue(UserProfile.class);
                    if (user != null && user.getHasEpiPen() && !user.getNeedsHelp()
                            && !userSnapshot.getKey().equals(currentUser.getUid())) {
                        float[] distance = new float[1];
                        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                user.getLatitude(), user.getLongitude(), distance);
                        if (distance[0] <= SEARCH_RADIUS) {
                            nearbyUsersWithEpiPen.add(user);
                        }
                    }
                }
                displayNearbyUsersOnMap();
                updateUsersCount();
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(requireContext(), "Failed to find nearby users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayNearbyUsersOnMap() {
        if (mMap == null) return;

        mMap.clear();
        for (UserProfile user : nearbyUsersWithEpiPen) {
            LatLng userLocation = new LatLng(user.getLatitude(), user.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title(user.getName() + " (Has EpiPen)")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
            mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("You")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private void updateUsersCount() {
        usersCountTextView.setText("Nearby users found: " + nearbyUsersWithEpiPen.size());
    }

    // MapView lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}