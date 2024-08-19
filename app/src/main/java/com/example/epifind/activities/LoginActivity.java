package com.example.epifind.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.epifind.R;
import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.util.Arrays;
import java.util.List;

/**
 * This activity handles the user login process.
 * It checks if a user is already logged in; if not, it triggers the Firebase authentication UI.
 * After successful login, it creates or updates the user's profile in the database.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private UserManager userManager;

    // Launcher for handling the result from Firebase authentication UI
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);

        userManager = UserManager.getInstance();

        // Check if the user is already signed in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // If not signed in, trigger the sign-in flow
            signIn();
        } else {
            // If signed in, check profile completeness and proceed
            checkProfileAndProceed();
        }
    }

    /**
     * Initiates the Firebase authentication UI for signing in.
     */
    private void signIn() {
        // Specify the authentication providers (Email, Phone, Google)
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch the sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.epifinlogo)
                .setTosAndPrivacyPolicyUrls("https://example.com", "https://example.com")
                .build();
        signInLauncher.launch(signInIntent);
    }

    /**
     * Handles the result from the Firebase authentication UI.
     * If successful, it creates or updates the user's profile.
     *
     * @param result the result of the Firebase authentication UI
     */
    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            // Sign-in succeeded
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                createOrUpdateUserProfile(user);
            }
        } else {
            // Sign-in failed
            Log.e(TAG, "Sign-in failed");
        }
    }

    /**
     * Creates or updates the user's profile in the Firebase Realtime Database.
     *
     * @param user the currently signed-in Firebase user
     */
    private void createOrUpdateUserProfile(FirebaseUser user) {
        String userId = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Check if the user's profile already exists
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile updatedProfile = dataSnapshot.exists()
                        ? updateExistingProfile(dataSnapshot, user)
                        : createNewProfile(user);
                updatedProfile.setUserId(userId);
                updateProfile(userRef, updatedProfile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user profile", databaseError.toException());
            }
        });
    }

    /**
     * Updates the existing user profile if necessary.
     *
     * @param dataSnapshot the current data snapshot of the user's profile
     * @param user the currently signed-in Firebase user
     * @return the updated user profile
     */
    private UserProfile updateExistingProfile(DataSnapshot dataSnapshot, FirebaseUser user) {
        UserProfile existingProfile = dataSnapshot.getValue(UserProfile.class);
        if (existingProfile != null && user.getDisplayName() != null
                && !user.getDisplayName().equals(existingProfile.getName())) {
            existingProfile.setName(user.getDisplayName());
        }
        return existingProfile != null ? existingProfile : createNewProfile(user);
    }

    /**
     * Creates a new user profile with the default values.
     *
     * @param user the currently signed-in Firebase user
     * @return a new user profile
     */
    private UserProfile createNewProfile(FirebaseUser user) {
        return new UserProfile(
                user.getDisplayName(),
                "",  // allergies
                "",  // epiPenExpiry
                0,   // latitude
                0,   // longitude
                false  // hasEpiPen
        );
    }

    /**
     * Updates the user's profile in the Firebase Realtime Database.
     *
     * @param userRef the database reference for the user's profile
     * @param profile the user profile to update
     */
    private void updateProfile(DatabaseReference userRef, UserProfile profile) {
        userRef.setValue(profile).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User profile updated successfully");
                checkProfileAndProceed();
            } else {
                Log.e(TAG, "Failed to update user profile", task.getException());
            }
        });
    }

    /**
     * Checks if the user's profile is complete and proceeds to the main activity.
     * If the profile is incomplete, it redirects to the profile setup.
     */
    private void checkProfileAndProceed() {
        userManager.isProfileComplete(isComplete -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            if (!isComplete) {
                intent.putExtra("openProfileFragment", true);
            }
            startActivity(intent);
            finish();
        });
    }
}
