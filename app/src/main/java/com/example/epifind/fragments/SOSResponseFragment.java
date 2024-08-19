package com.example.epifind.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.epifind.activities.MainActivity;
import com.example.epifind.databinding.FragmentSosResponseBinding;
import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SOSResponseFragment handles the user's response to an SOS request.
 * It displays the location of the requester on a map and provides options
 * for the user to either help or indicate that they cannot help.
 */
public class SOSResponseFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "SOSResponseFragment";
    private static final float DEFAULT_ZOOM = 15f;

    private FragmentSosResponseBinding binding;
    private String requesterId;
    private double latitude;
    private double longitude;
    private UserManager userManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extractArguments();
        userManager = UserManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSosResponseBinding.inflate(inflater, container, false);
        initializeMap(savedInstanceState);
        setupButtons();
        loadRequesterInfo();
        return binding.getRoot();
    }

    /**
     * Extracts the arguments passed to the fragment, such as the requester's ID and location.
     */
    private void extractArguments() {
        if (getArguments() != null) {
            requesterId = getArguments().getString("requesterId");
            latitude = getArguments().getDouble("latitude");
            longitude = getArguments().getDouble("longitude");
        }
    }

    /**
     * Initializes the map view and sets up the callback for when the map is ready.
     *
     * @param savedInstanceState The saved instance state of the fragment.
     */
    private void initializeMap(Bundle savedInstanceState) {
        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);
    }

    /**
     * Sets up the buttons for responding to the SOS request.
     */
    private void setupButtons() {
        binding.helpButton.setOnClickListener(v -> respondToSOS(true));
        binding.cannotHelpButton.setOnClickListener(v -> respondToSOS(false));
    }

    /**
     * Loads the requester's information and displays it in the UI.
     */
    private void loadRequesterInfo() {
        userManager.getUserProfileById(requesterId, new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                String info = String.format("%s needs help!", userProfile.getName());
                binding.userInfoTextView.setText(info);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load requester info: " + error);
                binding.userInfoTextView.setText("Someone needs help!");
            }
        });
    }

    /**
     * Sends the user's response to the SOS request.
     *
     * @param canHelp Indicates whether the user can help or not.
     */
    private void respondToSOS(boolean canHelp) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference responseRef = FirebaseDatabase.getInstance().getReference("sos_responses")
                .child(requesterId).child(currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("canHelp", canHelp);
        response.put("timestamp", ServerValue.TIMESTAMP);

        responseRef.setValue(response).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                handleSuccessfulResponse(canHelp);
            } else {
                Toast.makeText(getContext(), "Failed to send response", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handles the actions to take after the response has been successfully sent.
     *
     * @param canHelp Indicates whether the user can help or not.
     */
    private void handleSuccessfulResponse(boolean canHelp) {
        updateUserStatus();
        Toast.makeText(getContext(), "Response sent", Toast.LENGTH_SHORT).show();
        updateSOSRequest();
        removeSOSNotification();

        if (canHelp) {
            navigateToHome();
            openGoogleMapsNavigation();
        } else {
            navigateToHome();
        }
    }

    /**
     * Updates the user's status in the UserManager.
     */
    private void updateUserStatus() {
        userManager.updateUserResponseStatus(UserProfile.ResponseStatus.AVAILABLE, new UserManager.OnUserProfileUpdateListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "User status updated successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to update user status: " + error);
            }
        });
    }

    /**
     * Updates the SOS request to mark it as inactive.
     */
    private void updateSOSRequest() {
        DatabaseReference sosRequestRef = FirebaseDatabase.getInstance().getReference("sos_requests").child(requesterId);
        sosRequestRef.child("active").setValue(false);
    }

    /**
     * Removes the SOS notification for the current user.
     */
    private void removeSOSNotification() {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference sosNotificationRef = FirebaseDatabase.getInstance().getReference("sos_notifications").child(currentUserId);
        sosNotificationRef.removeValue();
    }

    /**
     * Opens Google Maps for navigation to the requester's location.
     */
    private void openGoogleMapsNavigation() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(getContext(), "Google Maps app is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigates the user back to the home screen.
     */
    private void navigateToHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToHome();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng sosLocation = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(sosLocation).title("SOS Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sosLocation, DEFAULT_ZOOM));
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.mapView.onSaveInstanceState(outState);
    }
}
