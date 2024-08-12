package com.example.epifind;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.epifind.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserProfile();
        }

        binding.buttonSave.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        mDatabase.child("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null) {
                        binding.editTextName.setText(userProfile.getName());
                        binding.editTextAllergies.setText(userProfile.getAllergies());
                        binding.editTextEpiPenExpiry.setText(userProfile.getEpiPenExpiry());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String name = binding.editTextName.getText().toString().trim();
        String allergies = binding.editTextAllergies.getText().toString().trim();
        String epiPenExpiry = binding.editTextEpiPenExpiry.getText().toString().trim();

        UserProfile userProfile = new UserProfile(name, allergies, epiPenExpiry);

        mDatabase.child("users").child(currentUser.getUid()).setValue(userProfile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to save profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
