package com.example.epifind.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.epifind.R;
import com.example.epifind.managers.SOSManager;
import com.example.epifind.adapters.UserAdapter;
import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;
import com.example.epifind.activities.MainActivity;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * SOSFragment is responsible for managing and displaying the SOS functionality in the application.
 * It displays nearby users with EpiPens on a map and handles SOS requests and responses.
 */
public class SOSFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "SOSFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private GoogleMap mMap;
    private UserAdapter userAdapter;
    private List<UserProfile> nearbyUsersWithEpiPen = new ArrayList<>();
    private DatabaseReference mDatabase;
    private Location currentLocation;
    private SOSManager sosManager;
    private TextView usersCountTextView;
    private long sosTimestamp;
    private SeekBar searchRadiusSeekBar;
    private TextView searchRadiusTextView;
    private float searchRadius; // in KM
    private static final float MIN_SEARCH_RADIUS = 1f; // 1 KM
    private static final float MAX_SEARCH_RADIUS = 10f; // 10 KM

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sos_fragment, container, false);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        RecyclerView usersRecyclerView = view.findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(nearbyUsersWithEpiPen);
        usersRecyclerView.setAdapter(userAdapter);

        searchRadiusSeekBar = view.findViewById(R.id.searchRadiusSeekBar);
        searchRadiusTextView = view.findViewById(R.id.searchRadiusTextView);

        setupSearchRadiusControl();

        usersCountTextView = view.findViewById(R.id.usersCountTextView);

        sosManager = new SOSManager(requireContext());
        sosManager.setSosFragment(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth.getInstance().getCurrentUser();

        view.findViewById(R.id.cancelSOSButton).setOnClickListener(v -> cancelSOS());

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
    }

    /**
     * Retrieves the current location of the user and finds nearby users with EpiPens.
     */
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

    /**
     * Finds nearby users with EpiPens and updates the UI with their information.
     */
    public void findNearbyUsersWithEpiPen() {
        if (currentLocation == null) return;

        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference sosRequestRef = FirebaseDatabase.getInstance().getReference("sos_requests").child(currentUserId);

        sosRequestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    sosTimestamp = dataSnapshot.child("timestamp").getValue(Long.class);
                    findNearbyUsersFromDatabase();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching SOS request", databaseError.toException());
            }
        });
    }

    /**
     * Retrieves the list of users from the database and filters those who have EpiPens and are within the search radius.
     */
    private void findNearbyUsersFromDatabase() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nearbyUsersWithEpiPen.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    UserProfile user = userSnapshot.getValue(UserProfile.class);
                    if (user != null && user.getHasEpiPen() && !user.getNeedsHelp()) {
                        user.setUserId(userSnapshot.getKey());
                        float[] distance = new float[1];
                        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                user.getLatitude(), user.getLongitude(), distance);
                        if (distance[0] <= searchRadius * 1000) { // new radius
                            nearbyUsersWithEpiPen.add(user);
                        }
                    }
                }
                updateUI();
                sosManager.notifyNearbyUsers(nearbyUsersWithEpiPen);

                setupSOSResponseListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to find nearby users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the UI elements such as the map and the list of nearby users.
     */
    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void updateUI() {
        userAdapter.notifyDataSetChanged();
        usersCountTextView.setText("Nearby users found: " + nearbyUsersWithEpiPen.size());
        updateMap();
    }

    /**
     * Updates the map with markers representing the locations of the nearby users with EpiPens.
     */
    private void updateMap() {
        if (mMap == null) return;

        mMap.clear();
        for (UserProfile user : nearbyUsersWithEpiPen) {
            LatLng userLocation = new LatLng(user.getLatitude(), user.getLongitude());
            float markerColor;
            switch (user.getResponseStatus()) {
                case RESPONDING:
                    markerColor = BitmapDescriptorFactory.HUE_GREEN;
                    break;
                case UNAVAILABLE:
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                    break;
                case AVAILABLE:
                default:
                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
            }
            mMap.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title(user.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        }

        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            float zoomLevel = calculateZoomLevel(searchRadius);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel));
        }
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
            mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("You")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

    }

    /**
     * Calculates the appropriate zoom level for the map based on the search radius.
     *
     * @param radius The search radius in kilometers.
     * @return The zoom level for the map.
     */
    private float calculateZoomLevel(float radius) {
        return (float) (16 - Math.log(radius) / Math.log(2));
    }

    /**
     * Cancels the SOS request and resets the nearby users' statuses.
     */
    private void cancelSOS() {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference sosRequestsRef = FirebaseDatabase.getInstance().getReference("sos_requests");
        DatabaseReference latestSosRef = FirebaseDatabase.getInstance().getReference("latest_sos");

        sosRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String requester = childSnapshot.child("requester").getValue(String.class);
                    if (requester != null && requester.equals(currentUserId)) {
                        childSnapshot.getRef().removeValue();

                        // Remove the latest_sos entry
                        latestSosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot latestSosSnapshot) {
                                String latestSosRequester = latestSosSnapshot.child("requester").getValue(String.class);
                                if (latestSosRequester != null && latestSosRequester.equals(currentUserId)) {
                                    latestSosRef.removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("cancelSOS", "Failed to remove latest_sos: " + databaseError.getMessage());
                            }
                        });
                    }
                }

                sosManager.updateSOSState(false, new UserManager.OnUserProfileUpdateListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "SOS Cancelled", Toast.LENGTH_SHORT).show();
                        resetNearbyUsersStatus();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToHome();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Failed to update user status: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to cancel SOS: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Resets the response status of nearby users to "AVAILABLE".
     */
    private void resetNearbyUsersStatus() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    userSnapshot.getRef().child("responseStatus").setValue(UserProfile.ResponseStatus.AVAILABLE.name());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SOSFragment", "Failed to reset users status", databaseError.toException());
            }
        });
    }

    /**
     * Sets up a listener for SOS responses from nearby users.
     */
    private void setupSOSResponseListener() {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference sosResponsesRef = FirebaseDatabase.getInstance().getReference("sos_responses").child(currentUserId);
        sosResponsesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                String responderId = dataSnapshot.getKey();
                Boolean canHelp = dataSnapshot.child("canHelp").getValue(Boolean.class);
                Long responseTimestamp = dataSnapshot.child("timestamp").getValue(Long.class);
                if (responderId != null && canHelp != null && responseTimestamp != null && responseTimestamp > sosTimestamp) {
                    updateNearbyUserStatus(responderId, canHelp);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Handle changes if necessary
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle removal if necessary
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                // Handle moves if necessary
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "SOS response listener cancelled", error.toException());
            }
        });
    }

    /**
     * Updates the response status of a nearby user based on their SOS response.
     *
     * @param userId  The ID of the user whose status needs to be updated.
     * @param canHelp Whether the user is able to help.
     */
    private void updateNearbyUserStatus(String userId, boolean canHelp) {
        for (UserProfile user : nearbyUsersWithEpiPen) {
            if (user.getUserId().equals(userId)) {
                user.setResponseStatus(canHelp ? UserProfile.ResponseStatus.RESPONDING : UserProfile.ResponseStatus.UNAVAILABLE);
                break;
            }
        }
        updateUI();
    }

    /**
     * Sets up the search radius control (SeekBar) for adjusting the radius of the search for nearby users.
     */
    private void setupSearchRadiusControl() {
        searchRadius = loadSearchRadius();
        int progress = (int) ((searchRadius - MIN_SEARCH_RADIUS) / (MAX_SEARCH_RADIUS - MIN_SEARCH_RADIUS) * 100);
        searchRadiusSeekBar.setProgress(progress);
        updateSearchRadiusDisplay();

        searchRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                searchRadius = MIN_SEARCH_RADIUS + (MAX_SEARCH_RADIUS - MIN_SEARCH_RADIUS) * progress / 100f;
                updateSearchRadiusDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSearchRadius(searchRadius);
                findNearbyUsersWithEpiPen();
            }
        });
    }

    /**
     * Updates the display of the search radius.
     */
    private void updateSearchRadiusDisplay() {
        searchRadiusTextView.setText(String.format(Locale.getDefault(), "Search Radius: %.1f km", searchRadius));
    }

    /**
     * Saves the search radius to SharedPreferences.
     *
     * @param radius The search radius to save.
     */
    private void saveSearchRadius(float radius) {
        SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("search_radius", radius);
        editor.apply();
    }

    /**
     * Loads the search radius from SharedPreferences.
     *
     * @return The saved search radius, or a default value of 2 km if not found.
     */
    private float loadSearchRadius() {
        SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
        return prefs.getFloat("search_radius", 2f); // 2 km default
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
