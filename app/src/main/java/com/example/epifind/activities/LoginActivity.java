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

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private UserManager userManager;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_main);

        userManager = UserManager.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            signIn();
        } else {
            checkProfileAndProceed();
        }
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.epifinlogo)
                .setTosAndPrivacyPolicyUrls("https://example.com", "https://example.com").build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                createOrUpdateUserProfile(user);
            }
        } else {
            Log.e(TAG, "Sign-in failed");
        }
    }

    private void createOrUpdateUserProfile(FirebaseUser user) {
        UserProfile newProfile = new UserProfile(
                user.getDisplayName(),
                "",  // allergies
                "",  // epiPenExpiry
                0,   // latitude
                0,   // longitude
                false  // hasEpiPen
        );

        userManager.createOrUpdateUser(newProfile, new UserManager.OnUserProfileUpdateListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "User profile created/updated successfully");
                checkProfileAndProceed();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to create/update user profile: " + error);
                // You might want to show an error message to the user here
            }
        });
    }

    private void checkProfileAndProceed() {
        userManager.isProfileComplete(isComplete -> {
            if (isComplete) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            } else {
                // Profile is incomplete, start MainActivity with instruction to open ProfileFragment
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("openProfileFragment", true);
                startActivity(intent);
            }
            finish();
        });
    }

}