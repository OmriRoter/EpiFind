package com.example.epifind.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.epifind.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        SwitchMaterial switchSosNotifications = view.findViewById(R.id.switchSosNotifications);
        SwitchMaterial switchLocationSharing = view.findViewById(R.id.switchLocationSharing);
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);
        MaterialButton buttonChangePassword = view.findViewById(R.id.buttonChangePassword);
        MaterialButton buttonSetEpiPenReminder = view.findViewById(R.id.buttonSetEpiPenReminder);
        View textViewPrivacyPolicy = view.findViewById(R.id.textViewPrivacyPolicy);


        // Set up listeners
        switchSosNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Implement SOS notifications toggle logic
            Toast.makeText(getContext(), "SOS Notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switchLocationSharing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Implement location sharing toggle logic
            Toast.makeText(getContext(), "Location sharing " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        buttonChangePassword.setOnClickListener(v -> {
            // TODO: Implement change password logic
            Toast.makeText(getContext(), "Change password functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        buttonSetEpiPenReminder.setOnClickListener(v -> {
            // TODO: Implement EpiPen reminder logic
            Toast.makeText(getContext(), "EpiPen reminder functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        textViewPrivacyPolicy.setOnClickListener(v -> {
            // TODO: Replace with actual privacy policy URL
            String url = "https://www.example.com/privacy-policy";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        return view;
    }
}