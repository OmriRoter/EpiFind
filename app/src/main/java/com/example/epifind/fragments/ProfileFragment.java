package com.example.epifind.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
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
import com.example.epifind.adapters.AllergyAdapter;
import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;
import com.example.epifind.activities.MainActivity;
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = UserManager.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        setupAllergyList();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();

        if (currentUser != null) {
            loadUserProfile();
        }

        Bundle args = getArguments();
        if (args != null && args.getBoolean("startInEditMode", false)) {
            enableEditMode();
        }
    }

    private void setupAllergyList() {
        allergyList = new ArrayList<>(Arrays.asList("Peanuts", "Tree nuts", "Milk", "Eggs", "Wheat", "Soy", "Fish", "Shellfish"));
        selectedAllergies = new boolean[allergyList.size()];
    }

    private void setupListeners() {
        if (binding != null) {
            binding.buttonSelectAllergies.setOnClickListener(v -> showAllergySelectionDialog());
            binding.buttonSelectEpiPenExpiry.setOnClickListener(v -> showDatePickerDialog());
            binding.buttonEditProfile.setOnClickListener(v -> enableEditMode());
            binding.buttonSaveProfile.setOnClickListener(v -> saveUserProfile());

            binding.switchHasEpipen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    binding.textViewEpiPenExpiry.setVisibility(View.VISIBLE);
                    binding.buttonSelectEpiPenExpiry.setVisibility(View.VISIBLE);
                } else {
                    binding.textViewEpiPenExpiry.setVisibility(View.GONE);
                    binding.buttonSelectEpiPenExpiry.setVisibility(View.GONE);
                }
            });
        }
    }


    private void loadUserProfile() {
        if (binding == null || !isAdded()) return;

        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .centerCrop()
                    .placeholder(R.drawable.epifinlogo)
                    .into(binding.mainIMGImage);
        }

        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (binding == null || !isAdded()) return;

                binding.mainLBLName.setText(userProfile.getName() != null ? userProfile.getName() : "");
                binding.mainLBLEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
                binding.mainLBLPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
                binding.textViewSelectedAllergies.setText(userProfile.getAllergies() != null ? userProfile.getAllergies() : "");
                binding.textViewEpiPenExpiry.setText(userProfile.getEpiPenExpiry() != null ? userProfile.getEpiPenExpiry() : "");
                selectedEpiPenExpiry = userProfile.getEpiPenExpiry() != null ? userProfile.getEpiPenExpiry() : "";

                boolean hasEpiPen = userProfile.getHasEpiPen();
                binding.switchHasEpipen.setChecked(hasEpiPen);
                if (hasEpiPen) {
                    binding.textViewEpiPenExpiry.setVisibility(View.VISIBLE);
                    binding.buttonSelectEpiPenExpiry.setVisibility(View.VISIBLE);
                } else {
                    binding.textViewEpiPenExpiry.setVisibility(View.GONE);
                    binding.buttonSelectEpiPenExpiry.setVisibility(View.GONE);
                }

                if (userProfile.getName() == null || userProfile.getName().isEmpty() ||
                        userProfile.getAllergies() == null || userProfile.getAllergies().isEmpty() ||
                        userProfile.getEpiPenExpiry() == null || userProfile.getEpiPenExpiry().isEmpty()) {
                    enableEditMode();
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateSelectedAllergies(String allergies) {
        if (allergies == null || allergies.isEmpty()) {
            Arrays.fill(selectedAllergies, false);
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
        Context context = getContext();
        if (context == null) return;

        // יצירת View מותאם אישית לדיאלוג
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_allergy_selection, null);
        ListView listView = dialogView.findViewById(R.id.allergy_list_view);

        // יצירת ה-Adapter המותאם אישית
        AllergyAdapter adapter = new AllergyAdapter(context, allergyList, selectedAllergies);
        listView.setAdapter(adapter);

        // יצירת ה-Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Allergies");
        builder.setView(dialogView);

        builder.setPositiveButton("OK", (dialog, which) -> updateSelectedAllergiesText());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSelectedAllergiesText() {
        if (binding == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedAllergies.length; i++) {
            if (selectedAllergies[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(allergyList.get(i));
            }
        }

        // עדכון טקסט האלרגיות שנבחרו
        binding.textViewSelectedAllergies.setText(sb.toString());
    }

    private void showDatePickerDialog() {
        Context context = getContext();
        if (context == null) return;

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedEpiPenExpiry = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    if (binding != null) {
                        binding.textViewEpiPenExpiry.setText(selectedEpiPenExpiry);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void enableEditMode() {
        if (binding == null) return;

        isEditMode = true;
        binding.editTextName.setVisibility(View.VISIBLE);
        binding.mainLBLName.setVisibility(View.GONE);
        binding.editTextName.setText(binding.mainLBLName.getText());

        // הצגת האפשרויות לעריכת פרטים
        binding.layoutEpipenQuestion.setVisibility(View.VISIBLE); // הצגת השאלה על ה-EpiPen
        binding.buttonSelectAllergies.setEnabled(true);
        binding.buttonSelectEpiPenExpiry.setEnabled(true);
        binding.buttonSaveProfile.setVisibility(View.VISIBLE);
        binding.buttonEditProfile.setVisibility(View.GONE);
    }

    private void disableEditMode() {
        if (binding == null) return;

        isEditMode = false;
        binding.editTextName.setVisibility(View.GONE);
        binding.mainLBLName.setVisibility(View.VISIBLE);
        binding.mainLBLName.setText(binding.editTextName.getText());

        // החזרת התצוגה למצב הצגת פרופיל בלבד
        binding.layoutEpipenQuestion.setVisibility(View.GONE);
        binding.buttonSelectAllergies.setEnabled(false);
        binding.buttonSelectEpiPenExpiry.setEnabled(false);
        binding.buttonSaveProfile.setVisibility(View.GONE);
        binding.buttonEditProfile.setVisibility(View.VISIBLE);
    }

    private void saveUserProfile() {
        if (binding == null || !isAdded()) return;

        String name = binding.editTextName.getText().toString().trim();
        String allergies = binding.textViewSelectedAllergies.getText().toString();
        String epiPenExpiry = binding.textViewEpiPenExpiry.getText().toString();
        boolean hasEpiPen = binding.switchHasEpipen.isChecked();

        if (name.isEmpty()) {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show();
            }
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
                        hasEpiPen // עדכון שדה ה-hasEpiPen בהתאם למתג
                );

                userManager.createOrUpdateUser(updatedProfile, new UserManager.OnUserProfileUpdateListener() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                                disableEditMode();
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).navigateToHome();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (isAdded()) {
                            Context context = getContext();
                            if (context != null) {
                                Toast.makeText(context, "Failed to save profile: " + error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "Failed to fetch existing profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
