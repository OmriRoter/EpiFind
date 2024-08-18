package com.example.epifind.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.epifind.R;
import com.example.epifind.activities.MainActivity;
import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
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

public class SOSResponseFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "SOSResponseFragment";
    private static final float DEFAULT_ZOOM = 15f;

    private String requesterId;
    private double latitude;
    private double longitude;
    private MapView mapView;
    private UserManager userManager;
    private TextView userInfoTextView;
    private Button helpButton;
    private Button cannotHelpButton;

    public SOSResponseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requesterId = getArguments().getString("requesterId");
            latitude = getArguments().getDouble("latitude");
            longitude = getArguments().getDouble("longitude");
        }
        userManager = UserManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sos_response, container, false);

        userInfoTextView = view.findViewById(R.id.userInfoTextView);
        mapView = view.findViewById(R.id.mapView);
        helpButton = view.findViewById(R.id.helpButton);
        cannotHelpButton = view.findViewById(R.id.cannotHelpButton);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setupButtons();
        loadRequesterInfo();

        return view;
    }

    private void setupButtons() {
        helpButton.setOnClickListener(v -> respondToSOS(true));
        cannotHelpButton.setOnClickListener(v -> respondToSOS(false));
    }

    private void loadRequesterInfo() {
        userManager.getUserProfileById(requesterId, new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                String info = String.format("%s needs help!", userProfile.getName());
                userInfoTextView.setText(info);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load requester info: " + error);
                userInfoTextView.setText("Someone needs help!");
            }
        });
    }

    private void respondToSOS(boolean canHelp) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference responseRef = FirebaseDatabase.getInstance().getReference("sos_responses")
                .child(requesterId).child(currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("canHelp", canHelp);
        response.put("timestamp", ServerValue.TIMESTAMP);

        responseRef.setValue(response).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Update current user status to AVAILABLE
                updateUserStatus(UserProfile.ResponseStatus.AVAILABLE);
                Toast.makeText(getContext(), "Response sent", Toast.LENGTH_SHORT).show();

                // Update SOS request to inactive
                DatabaseReference sosRequestRef = FirebaseDatabase.getInstance().getReference("sos_requests").child(requesterId);
                sosRequestRef.child("active").setValue(false);

                // Remove the notification for the current user
                DatabaseReference sosNotificationRef = FirebaseDatabase.getInstance().getReference("sos_notifications").child(currentUserId);
                sosNotificationRef.removeValue();

                if (canHelp) {
                    navigateToHome();
                    openGoogleMapsNavigation();
                } else {
                    navigateToHome();
                }
            } else {
                Toast.makeText(getContext(), "Failed to send response", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserStatus(UserProfile.ResponseStatus status) {
        userManager.updateUserResponseStatus(status, new UserManager.OnUserProfileUpdateListener() {
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
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}