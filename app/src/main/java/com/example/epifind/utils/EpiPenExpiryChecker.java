package com.example.epifind.utils;

import android.util.Log;

import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EpiPenExpiryChecker {
    private static final String TAG = "EpiPenExpiryChecker";

    public interface ExpiryCheckListener {
        void onExpiryCheck(int daysUntilExpiry);
    }

    public static void checkEpiPenExpiry(UserManager userManager, ExpiryCheckListener listener) {
        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                String expiryDate = userProfile.getEpiPenExpiry();
                if (expiryDate != null && !expiryDate.isEmpty()) {
                    int daysUntilExpiry = getDaysUntilExpiry(expiryDate);
                    listener.onExpiryCheck(daysUntilExpiry);
                } else {
                    listener.onExpiryCheck(-1); // Indicate no expiry date set
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch user profile: " + error);
                listener.onExpiryCheck(-1); // Indicate error
            }
        });
    }

    private static int getDaysUntilExpiry(String expiryDateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date expiryDate = sdf.parse(expiryDateString);
            Date currentDate = new Date();
            long diffInMillies = expiryDate.getTime() - currentDate.getTime();
            return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)+1;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing expiry date", e);
            return -1; // Return -1 if there's an error parsing the date
        }
    }
}
