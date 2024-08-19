package com.example.epifind.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

import com.example.epifind.R;
import com.example.epifind.activities.MainActivity;

/**
 * LocalNotificationManager is responsible for managing local notifications in the EpiFind app.
 * It handles the creation of notification channels and the display of notifications to the user.
 */
public class LocalNotificationManager {
    private static final String CHANNEL_ID = "EpiFind_Channel";
    private static final String CHANNEL_NAME = "EpiFind Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for EpiFind app";
    private static final int NOTIFICATION_ID = 1;

    private final Context context;
    private final NotificationManager notificationManager;

    /**
     * Constructor for the LocalNotificationManager.
     *
     * @param context The application context used to create notifications.
     */
    public LocalNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    /**
     * Creates a notification channel required for Android versions Oreo and above.
     */
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(CHANNEL_DESCRIPTION);
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Displays a notification with the specified title and message.
     *
     * @param title   The title of the notification.
     * @param message The message body of the notification.
     */
    public void showNotification(String title, String message) {
        Intent intent = createIntent();
        PendingIntent pendingIntent = createPendingIntent(intent);
        NotificationCompat.Builder builder = createNotificationBuilder(title, message, pendingIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Creates an intent to open the MainActivity when the notification is clicked.
     *
     * @return The intent that opens MainActivity.
     */
    private Intent createIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    /**
     * Creates a PendingIntent for the notification that will trigger the provided intent.
     *
     * @param intent The intent to trigger when the notification is clicked.
     * @return The PendingIntent associated with the notification.
     */
    private PendingIntent createPendingIntent(Intent intent) {
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Creates a NotificationCompat.Builder for constructing the notification.
     *
     * @param title         The title of the notification.
     * @param message       The message body of the notification.
     * @param pendingIntent The PendingIntent to trigger when the notification is clicked.
     * @return The NotificationCompat.Builder used to build the notification.
     */
    private NotificationCompat.Builder createNotificationBuilder(String title, String message, PendingIntent pendingIntent) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
    }

    /**
     * Cancels all notifications that have been issued by this NotificationManager.
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}
