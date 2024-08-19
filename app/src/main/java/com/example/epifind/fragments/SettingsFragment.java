package com.example.epifind.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.epifind.R;
import com.example.epifind.databinding.FragmentSettingsBinding;

/**
 * SettingsFragment allows users to adjust app settings such as SOS notifications,
 * location sharing, dark mode, and more. It also provides access to the privacy policy.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        setupListeners();
        return binding.getRoot();
    }

    /**
     * Sets up the event listeners for the settings options.
     */
    private void setupListeners() {
        binding.switchSosNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleSosNotificationsToggle(isChecked));

        binding.switchLocationSharing.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleLocationSharingToggle(isChecked));

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleDarkModeToggle(isChecked));

        binding.buttonChangePassword.setOnClickListener(v -> handleChangePassword());

        binding.buttonSetEpiPenReminder.setOnClickListener(v -> handleSetEpiPenReminder());

        binding.textViewPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
    }

    /**
     * Handles the toggle for SOS notifications.
     *
     * @param isChecked Whether SOS notifications are enabled or disabled.
     */
    private void handleSosNotificationsToggle(boolean isChecked) {
        // TODO: Implement SOS notifications toggle logic
        showToast("SOS Notifications " + (isChecked ? "enabled" : "disabled"));
    }

    /**
     * Handles the toggle for location sharing.
     *
     * @param isChecked Whether location sharing is enabled or disabled.
     */
    private void handleLocationSharingToggle(boolean isChecked) {
        // TODO: Implement location sharing toggle logic
        showToast("Location sharing " + (isChecked ? "enabled" : "disabled"));
    }

    /**
     * Handles the toggle for dark mode.
     *
     * @param isChecked Whether dark mode is enabled or disabled.
     */
    private void handleDarkModeToggle(boolean isChecked) {
        AppCompatDelegate.setDefaultNightMode(isChecked ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Handles the event when the user clicks the "Change Password" button.
     */
    private void handleChangePassword() {
        // TODO: Implement change password logic
        showToast("Change password functionality coming soon");
    }

    /**
     * Handles the event when the user clicks the "Set EpiPen Reminder" button.
     */
    private void handleSetEpiPenReminder() {
        // TODO: Implement EpiPen reminder logic
        showToast("EpiPen reminder functionality coming soon");
    }

    /**
     * Opens the privacy policy in a web browser.
     */
    private void openPrivacyPolicy() {
        // TODO: Replace with actual privacy policy URL
        String url = "https://www.example.com/privacy-policy";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    /**
     * Displays a toast message with the provided text.
     *
     * @param message The message to display in the toast.
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
