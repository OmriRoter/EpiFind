package com.example.epifind;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class SOSManager {
    private Context context;
    private Vibrator vibrator;
    private Handler handler;
    private static final long VIBRATION_DURATION = 3000; // 3 seconds
    private static final long VIBRATION_INTERVAL = 500; // 0.5 seconds
    private boolean isActivating = false;
    private UserManager userManager;


    public interface SOSActivationListener {
        void onSOSActivationStarted();
        void onSOSActivationCancelled();
        void onSOSActivated();
    }

    public SOSManager(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.userManager = UserManager.getInstance();

    }

    public void startSOSActivation(SOSActivationListener listener) {
        isActivating = true;
        listener.onSOSActivationStarted();

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, VIBRATION_INTERVAL, VIBRATION_INTERVAL};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        }

        handler.postDelayed(() -> {
            if (isActivating) {
                if (vibrator != null) {
                    vibrator.cancel();
                }
                listener.onSOSActivated();
                isActivating = false;
            }
        }, VIBRATION_DURATION);
    }

    public void cancelSOSActivation(SOSActivationListener listener) {
        if (isActivating) {
            handler.removeCallbacksAndMessages(null);
            if (vibrator != null) {
                vibrator.cancel();
            }
            listener.onSOSActivationCancelled();
            isActivating = false;
        }
    }

    public boolean isActivating() {
        return isActivating;
    }

    public void updateSOSState(boolean needsHelp, final UserManager.OnUserProfileUpdateListener listener) {
        userManager.getUserProfile(new UserManager.OnUserProfileFetchListener() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                userProfile.setNeedsHelp(needsHelp);
                userManager.createOrUpdateUser(userProfile, listener);
            }

            @Override
            public void onFailure(String error) {
                if (listener != null) {
                    listener.onFailure("Failed to fetch user profile: " + error);
                }
            }
        });
    }
}