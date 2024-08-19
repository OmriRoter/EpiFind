package com.example.epifind.utils;

import android.util.Log;

import com.example.epifind.managers.UserManager;
import com.example.epifind.models.UserProfile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * EpiPenExpiryChecker is a utility class that checks the expiry date of an EpiPen.
 * It calculates the number of days remaining until the EpiPen expires and notifies the result
 * through a listener.
 */
public class EpiPenExpiryChecker {
    private static final String TAG = "EpiPenExpiryChecker";
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final int ERROR_CODE = -1;

    /**
     * ExpiryCheckListener is an interface used to handle the result of the expiry date check.
     */
    public interface ExpiryCheckListener {
        void onExpiryCheck(int daysUntilExpiry);
    }

    /**
     * Checks the expiry date of the EpiPen for the current user.
     *
     * @param userManager The UserManager instance used to retrieve the user's profile.
     * @param listener    The listener that will be notified with the result of the expiry check.
     */
    public static void checkEpiPenExpiry(UserManager userManager, ExpiryCheckListener listener) {
        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                String expiryDate = userProfile.getEpiPenExpiry();
                int daysUntilExpiry = (expiryDate != null && !expiryDate.isEmpty())
                        ? getDaysUntilExpiry(expiryDate)
                        : ERROR_CODE;
                listener.onExpiryCheck(daysUntilExpiry);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch user profile: " + error);
                listener.onExpiryCheck(ERROR_CODE);
            }
        });
    }

    /**
     * Calculates the number of days remaining until the EpiPen expires based on the provided expiry date string.
     *
     * @param expiryDateString The expiry date as a string in the format "dd/MM/yyyy".
     * @return The number of days until the EpiPen expires, or ERROR_CODE if the date is invalid or cannot be parsed.
     */
    private static int getDaysUntilExpiry(String expiryDateString) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        try {
            Date expiryDate = sdf.parse(expiryDateString);
            Date currentDate = new Date();
            if (expiryDate == null) {
                throw new ParseException("Failed to parse expiry date", 0);
            }
            long diffInMillies = expiryDate.getTime() - currentDate.getTime();
            return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing expiry date", e);
            return ERROR_CODE;
        }
    }
}
