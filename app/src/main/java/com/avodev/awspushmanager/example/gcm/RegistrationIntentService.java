package com.avodev.awspushmanager.example.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.avodev.awspushmanager.AWSPushManager;
import com.avodev.awspushmanager.example.R;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {
    private static final String[] TOPICS = {"global"};

    public static final String BROADCAST_REGISTRATION_SUCCESSFUL = "registrationSuccessful";
    public static final String BROADCAST_REGISTRATION_FAILED = "registrationFailed";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public RegistrationIntentService() {
        super("AppRegistrationIntentService");
        Log.v("GCM", "started registration intent service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.v("GCM", "GCM Registration Token: " + token);

            // Implement this method to send any registration to your app's servers (we will register with AWS here)
            sendRegistrationToServer(token);

            // Subscribe to topic channels (not used for AWS)
            // subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            // Not used for AWS in this example
            // sharedPreferences.edit().putBoolean("sentTokenToServer", true).apply();
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d("GCM", "Failed to complete token refresh", e);

            // Send broadcast with error
            Intent i = new Intent(BROADCAST_REGISTRATION_FAILED);
            i.putExtra("error", e.getLocalizedMessage());
            LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(i);

            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            // Not used for AWS in this example
            // sharedPreferences.edit().putBoolean("sentTokenToServer", false).apply();
        }

        // Notify UI that registration has completed, so the progress indicator can be hidden (not used for AWS, since we need to wait for Amazon first)
        // Intent registrationComplete = new Intent("registrationComplete");
        // LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) throws Exception {
        // Register with Amazon
        AWSPushManager.sendRegistrationToServer(token, new AWSPushManager.Callback() {
            @Override
            public void onSuccess() {
                // Send broadcast
                Intent i = new Intent(BROADCAST_REGISTRATION_SUCCESSFUL);
                LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(i);
            }

            @Override
            public void onError(String error) {
                // Send broadcast with error
                Intent i = new Intent(BROADCAST_REGISTRATION_FAILED);
                i.putExtra("error", error);
                LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(i);
            }
        });
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
