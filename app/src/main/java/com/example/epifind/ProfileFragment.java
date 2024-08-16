package com.example.epifind;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.epifind.databinding.FragmentProfileBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private UserManager userManager;
    private FirebaseUser currentUser;
    private boolean[] selectedAllergies;
    private List<String> allergyList;
    private String selectedEpiPenExpiry = "";
    private boolean isEditMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        userManager = UserManager.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupAllergyList();
        setupListeners();

        if (currentUser != null) {
            loadUserProfile();
        }

        // Check if we need to start in edit mode (for new users or incomplete profiles)
        Bundle args = getArguments();
        if (args != null && args.getBoolean("startInEditMode", false)) {
            enableEditMode();
        }

        return view;
    }

    private void setupAllergyList() {
        allergyList = new ArrayList<>(Arrays.asList("Peanuts", "Tree nuts", "Milk", "Eggs", "Wheat", "Soy", "Fish", "Shellfish"));
        selectedAllergies = new boolean[allergyList.size()];
    }

    private void setupListeners() {
        binding.buttonSelectAllergies.setOnClickListener(v -> showAllergySelectionDialog());
        binding.buttonSelectEpiPenExpiry.setOnClickListener(v -> showDatePickerDialog());
        binding.buttonEditProfile.setOnClickListener(v -> enableEditMode());
        binding.buttonSaveProfile.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .centerCrop()
                    .placeholder(R.drawable.epifinlogo)
                    .into(binding.mainIMGImage);
        }

        // Load user data
        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                binding.mainLBLName.setText(userProfile.getName() != null ? userProfile.getName() : "");
                binding.mainLBLEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
                binding.mainLBLPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
                binding.textViewSelectedAllergies.setText(userProfile.getAllergies() != null ? userProfile.getAllergies() : "");
                binding.textViewEpiPenExpiry.setText(userProfile.getEpiPenExpiry() != null ? userProfile.getEpiPenExpiry() : "");
                selectedEpiPenExpiry = userProfile.getEpiPenExpiry() != null ? userProfile.getEpiPenExpiry() : "";
                if (userProfile.getAllergies() != null) {
                    updateSelectedAllergies(userProfile.getAllergies());
                }

                // If the profile is incomplete, start in edit mode
                if (userProfile.getName() == null || userProfile.getName().isEmpty() ||
                        userProfile.getAllergies() == null || userProfile.getAllergies().isEmpty() ||
                        userProfile.getEpiPenExpiry() == null || userProfile.getEpiPenExpiry().isEmpty()) {
                    enableEditMode();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Failed to load profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedAllergies(String allergies) {
        if (allergies == null || allergies.isEmpty()) {
            for (int i = 0; i < selectedAllergies.length; i++) {
                selectedAllergies[i] = false;
            }
            return;
        }

        String[] selectedAllergiesArray = allergies.split(", ");
        for (int i = 0; i < allergyList.size(); i++) {
            selectedAllergies[i] = false;
            for (String selectedAllergy : selectedAllergiesArray) {
                if (allergyList.get(i).equals(selectedAllergy)) {
                    selectedAllergies[i] = true;
                    break;
                }
            }
        }
    }

    private void showAllergySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Allergies");
        builder.setMultiChoiceItems(allergyList.toArray(new String[0]), selectedAllergies, (dialog, which, isChecked) -> {
            selectedAllergies[which] = isChecked;
        });
        builder.setPositiveButton("OK", (dialog, which) -> updateSelectedAllergiesText());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSelectedAllergiesText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedAllergies.length; i++) {
            if (selectedAllergies[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(allergyList.get(i));
            }
        }
        binding.textViewSelectedAllergies.setText(sb.toString());
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedEpiPenExpiry = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    binding.textViewEpiPenExpiry.setText(selectedEpiPenExpiry);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void enableEditMode() {
        isEditMode = true;
        binding.editTextName.setVisibility(View.VISIBLE);
        binding.mainLBLName.setVisibility(View.GONE);
        binding.editTextName.setText(binding.mainLBLName.getText());
        binding.buttonSelectAllergies.setEnabled(true);
        binding.buttonSelectEpiPenExpiry.setEnabled(true);
        binding.buttonSaveProfile.setVisibility(View.VISIBLE);
        binding.buttonEditProfile.setVisibility(View.GONE);
    }

    private void disableEditMode() {
        isEditMode = false;
        binding.editTextName.setVisibility(View.GONE);
        binding.mainLBLName.setVisibility(View.VISIBLE);
        binding.mainLBLName.setText(binding.editTextName.getText());
        binding.buttonSelectAllergies.setEnabled(false);
        binding.buttonSelectEpiPenExpiry.setEnabled(false);
        binding.buttonSaveProfile.setVisibility(View.GONE);
        binding.buttonEditProfile.setVisibility(View.VISIBLE);
    }

    private void saveUserProfile() {
        String name = binding.editTextName.getText().toString().trim();
        String allergies = binding.textViewSelectedAllergies.getText().toString();
        String epiPenExpiry = binding.textViewEpiPenExpiry.getText().toString();
        boolean hasEpiPen = !epiPenExpiry.isEmpty();

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
                        epiPenExpiry,
                        existingProfile.getLatitude(),
                        existingProfile.getLongitude(),
                        hasEpiPen
                );

                userManager.createOrUpdateUser(updatedProfile, new UserManager.OnUserProfileUpdateListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
                        disableEditMode();
                        // Navigate to HomeFragment
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToHome();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Failed to save profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Failed to fetch existing profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}