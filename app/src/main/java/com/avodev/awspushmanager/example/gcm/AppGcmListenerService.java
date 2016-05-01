package com.avodev.awspushmanager.example.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.avodev.awspushmanager.example.R;
import com.avodev.awspushmanager.example.activity.MainActivity;
import com.avodev.awspushmanager.example.util.SharedPreferencesKeys;
import com.google.android.gms.gcm.GcmListenerService;

import java.util.Random;

public class AppGcmListenerService extends GcmListenerService {
    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d("GCM", "Message received from: " + from + " with data: " + data);

        // Check if notifications are enabled
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SharedPreferencesKeys.PREFERENCE_NOTIFICATIONS, true)) {
            // Get data
            String message = data.getString("message");

            // Send notification
            sendNotification(message);
        }
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        // Create pending intent
        PendingIntent pendingIntent = null;

        // Create intent
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(this, 1000 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        // Build notification
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        // White icon for lollipop
        if (Build.VERSION.SDK_INT >= 21) {
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        }

        // Send notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(new Random().nextInt() /* ID of notification */, notificationBuilder.build());
    }
}
