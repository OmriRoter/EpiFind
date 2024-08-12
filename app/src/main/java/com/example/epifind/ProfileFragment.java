package com.example.epifind;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.epifind.databinding.FragmentProfileBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private boolean[] selectedAllergies;
    private List<String> allergyList;
    private String selectedEpiPenExpiry = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupAllergyList();
        setupListeners();

        if (currentUser != null) {
            loadUserProfile();
        }

        return view;
    }

    private void setupAllergyList() {
        allergyList = new ArrayList<>();
        allergyList.add("Peanuts");
        allergyList.add("Tree Nuts");
        allergyList.add("Milk");
        allergyList.add("Egg");
        allergyList.add("Wheat");
        allergyList.add("Soy");
        allergyList.add("Fish");
        allergyList.add("Shellfish");
        selectedAllergies = new boolean[allergyList.size()];
    }

    private void setupListeners() {
        binding.buttonSelectAllergies.setOnClickListener(v -> showAllergySelectionDialog());
        binding.buttonSelectEpiPenExpiry.setOnClickListener(v -> showDatePickerDialog());
        binding.buttonEditProfile.setOnClickListener(v -> enableEditMode());
        binding.buttonSaveProfile.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        // Load user details from Firebase Auth
        Glide.with(this)
                .load(currentUser.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.epifinlogo)
                .into(binding.mainIMGImage);
        binding.mainLBLName.setText(currentUser.getDisplayName());
        binding.mainLBLEmail.setText(currentUser.getEmail());
        binding.mainLBLPhone.setText(currentUser.getPhoneNumber());
        binding.mainLBLUid.setText(currentUser.getUid());

        // Load additional user data from Realtime Database
        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null) {
                        binding.textViewSelectedAllergies.setText(userProfile.getAllergies());
                        binding.textViewEpiPenExpiry.setText(userProfile.getEpiPenExpiry());
                        selectedEpiPenExpiry = userProfile.getEpiPenExpiry();
                        updateSelectedAllergies(userProfile.getAllergies());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedAllergies(String allergies) {
        String[] selectedAllergiesArray = allergies.split(", ");
        for (int i = 0; i < allergyList.size(); i++) {
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
        binding.buttonSelectAllergies.setEnabled(true);
        binding.buttonSelectEpiPenExpiry.setEnabled(true);
        binding.buttonSaveProfile.setVisibility(View.VISIBLE);
        binding.buttonEditProfile.setVisibility(View.GONE);
    }

    private void disableEditMode() {
        binding.buttonSelectAllergies.setEnabled(false);
        binding.buttonSelectEpiPenExpiry.setEnabled(false);
        binding.buttonSaveProfile.setVisibility(View.GONE);
        binding.buttonEditProfile.setVisibility(View.VISIBLE);
    }

    private void saveUserProfile() {
        String allergies = binding.textViewSelectedAllergies.getText().toString();
        String epiPenExpiry = binding.textViewEpiPenExpiry.getText().toString();

        UserProfile userProfile = new UserProfile(currentUser.getDisplayName(), allergies, epiPenExpiry);

        mDatabase.child("users").child(currentUser.getUid()).setValue(userProfile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
                        disableEditMode();
                    } else {
                        Toast.makeText(getContext(), "Failed to save profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}