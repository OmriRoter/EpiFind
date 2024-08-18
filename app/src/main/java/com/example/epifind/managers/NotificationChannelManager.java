package com.example.epifind.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

public class NotificationChannelManager {
    public static void createNotificationChannel(Context context) {
        CharSequence name = "SOS Notifications";
        String description = "Notifications for SOS requests";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel("SOS_CHANNEL", name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}

