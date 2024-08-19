package com.example.epifind.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.epifind.R;
import com.example.epifind.activities.LoginActivity;
import com.example.epifind.adapters.AllergyAdapter;
import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;
import com.example.epifind.activities.MainActivity;
import com.example.epifind.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * ProfileFragment allows users to view and edit their profile information,
 * including name, allergies, and EpiPen expiry date. It also handles user logout.
 */
public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private UserManager userManager;
    private FirebaseUser currentUser;
    private boolean[] selectedAllergies;
    private List<String> allergyList;
    private String selectedEpiPenExpiry = "";
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeComponents();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        loadUserProfile();
        checkStartInEditMode();
    }

    /**
     * Initializes the components needed for the profile, including Firebase authentication and user manager.
     */
    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        userManager = UserManager.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        setupAllergyList();
    }

    /**
     * Sets up the list of possible allergies that the user can select.
     */
    private void setupAllergyList() {
        allergyList = new ArrayList<>(Arrays.asList("Peanuts", "Tree nuts", "Milk", "Eggs", "Wheat", "Soy", "Fish", "Shellfish"));
        selectedAllergies = new boolean[allergyList.size()];
    }

    /**
     * Sets up the views and their associated event listeners.
     */
    private void setupViews() {
        if (binding != null) {
            binding.buttonSelectAllergies.setOnClickListener(v -> showAllergySelectionDialog());
            binding.buttonSelectEpiPenExpiry.setOnClickListener(v -> showDatePickerDialog());
            binding.buttonEditProfile.setOnClickListener(v -> enableEditMode());
            binding.buttonSaveProfile.setOnClickListener(v -> saveUserProfile());
            binding.buttonLogout.setOnClickListener(v -> logout());
            binding.switchHasEpipen.setOnCheckedChangeListener((buttonView, isChecked) -> toggleEpiPenViews(isChecked));
        }
    }

    /**
     * Toggles the visibility of the EpiPen-related views based on whether the user has an EpiPen.
     *
     * @param isChecked Whether the user has an EpiPen.
     */
    private void toggleEpiPenViews(boolean isChecked) {
        int visibility = isChecked ? View.VISIBLE : View.GONE;
        binding.textViewEpiPenExpiry.setVisibility(visibility);
        binding.buttonSelectEpiPenExpiry.setVisibility(visibility);
    }

    /**
     * Loads the current user's profile from the UserManager and updates the UI accordingly.
     */
    private void loadUserProfile() {
        if (binding == null || !isAdded() || currentUser == null) return;

        loadUserImage();
        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (binding == null || !isAdded()) return;
                updateUIWithUserProfile(userProfile);
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Loads the user's profile image using Glide and displays it in the UI.
     */
    private void loadUserImage() {
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .centerCrop()
                    .placeholder(R.drawable.epifinlogo)
                    .into(binding.mainIMGImage);
        }
    }

    /**
     * Updates the UI elements with the user's profile data.
     *
     * @param userProfile The UserProfile object containing the user's information.
     */
    private void updateUIWithUserProfile(UserProfile userProfile) {
        binding.mainLBLName.setText(userProfile.getName() != null ? userProfile.getName() : "");
        binding.mainLBLEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        binding.mainLBLPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        binding.textViewSelectedAllergies.setText(userProfile.getAllergies() != null ? userProfile.getAllergies() : "");
        binding.textViewEpiPenExpiry.setText(userProfile.getEpiPenExpiry() != null ? userProfile.getEpiPenExpiry() : "");
        selectedEpiPenExpiry = userProfile.getEpiPenExpiry() != null ? userProfile.getEpiPenExpiry() : "";

        boolean hasEpiPen = userProfile.getHasEpiPen();
        binding.switchHasEpipen.setChecked(hasEpiPen);
        toggleEpiPenViews(hasEpiPen);

        if (isProfileIncomplete(userProfile)) {
            enableEditMode();
        }
    }

    /**
     * Checks if the user's profile is incomplete and returns true if it is.
     *
     * @param userProfile The UserProfile object containing the user's information.
     * @return True if the profile is incomplete, false otherwise.
     */
    private boolean isProfileIncomplete(UserProfile userProfile) {
        return userProfile.getName() == null || userProfile.getName().isEmpty() ||
                userProfile.getAllergies() == null || userProfile.getAllergies().isEmpty() ||
                userProfile.getEpiPenExpiry() == null || userProfile.getEpiPenExpiry().isEmpty();
    }

    /**
     * Checks if the fragment should start in edit mode, based on arguments passed to it.
     */
    private void checkStartInEditMode() {
        Bundle args = getArguments();
        if (args != null && args.getBoolean("startInEditMode", false)) {
            enableEditMode();
        }
    }

    /**
     * Shows a dialog that allows the user to select their allergies from a list.
     */
    private void showAllergySelectionDialog() {
        Context context = getContext();
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_allergy_selection, null);
        ListView listView = dialogView.findViewById(R.id.allergy_list_view);
        listView.setAdapter(new AllergyAdapter(context, allergyList, selectedAllergies));

        new AlertDialog.Builder(context)
                .setTitle("Select Allergies")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> updateSelectedAllergiesText())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Updates the text view with the selected allergies.
     */
    private void updateSelectedAllergiesText() {
        if (binding == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedAllergies.length; i++) {
            if (selectedAllergies[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(allergyList.get(i));
            }
        }
        binding.textViewSelectedAllergies.setText(sb.toString());
    }

    /**
     * Shows a date picker dialog to allow the user to select their EpiPen expiry date.
     */
    private void showDatePickerDialog() {
        Context context = getContext();
        if (context == null) return;

        Calendar c = Calendar.getInstance();
        new DatePickerDialog(context,
                (view, year, monthOfYear, dayOfMonth) -> {
                    selectedEpiPenExpiry = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                    if (binding != null) {
                        binding.textViewEpiPenExpiry.setText(selectedEpiPenExpiry);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Enables the edit mode, allowing the user to modify their profile information.
     */
    private void enableEditMode() {
        if (binding == null) return;

        binding.editTextName.setVisibility(View.VISIBLE);
        binding.mainLBLName.setVisibility(View.GONE);
        binding.editTextName.setText(binding.mainLBLName.getText());

        binding.layoutEpipenQuestion.setVisibility(View.VISIBLE);
        binding.buttonSelectAllergies.setEnabled(true);
        binding.buttonSelectEpiPenExpiry.setEnabled(true);
        binding.buttonSaveProfile.setVisibility(View.VISIBLE);
        binding.buttonEditProfile.setVisibility(View.GONE);
    }

    /**
     * Disables the edit mode and saves the modified profile information.
     */
    private void disableEditMode() {
        if (binding == null) return;

        binding.editTextName.setVisibility(View.GONE);
        binding.mainLBLName.setVisibility(View.VISIBLE);
        binding.mainLBLName.setText(binding.editTextName.getText());

        binding.layoutEpipenQuestion.setVisibility(View.GONE);
        binding.buttonSelectAllergies.setEnabled(false);
        binding.buttonSelectEpiPenExpiry.setEnabled(false);
        binding.buttonSaveProfile.setVisibility(View.GONE);
        binding.buttonEditProfile.setVisibility(View.VISIBLE);
    }

    /**
     * Saves the user's profile information to the database.
     */
    private void saveUserProfile() {
        if (binding == null || !isAdded()) return;

        String name = binding.editTextName.getText().toString().trim();
        String allergies = binding.textViewSelectedAllergies.getText().toString();
        String epiPenExpiry = binding.textViewEpiPenExpiry.getText().toString();
        boolean hasEpiPen = binding.switchHasEpipen.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile existingProfile) {
                UserProfile updatedProfile = new UserProfile(
                        name,
                        allergies,
                        hasEpiPen ? epiPenExpiry : "",
                        existingProfile.getLatitude(),
                        existingProfile.getLongitude(),
                        hasEpiPen
                );

                userManager.createOrUpdateUser(updatedProfile, new UserManager.OnUserProfileUpdateListener() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
                            disableEditMode();
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).navigateToHome();
                            }
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to save profile: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to fetch existing profile: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Logs the user out and navigates them to the LoginActivity.
     */
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
