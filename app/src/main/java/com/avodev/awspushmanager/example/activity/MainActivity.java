package com.avodev.awspushmanager.example.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.avodev.awspushmanager.AWSPushManager;
import com.avodev.awspushmanager.AWSPushTopic;
import com.avodev.awspushmanager.example.R;
import com.avodev.awspushmanager.example.gcm.RegistrationIntentService;
import com.avodev.awspushmanager.example.util.Configuration;
import com.avodev.awspushmanager.example.util.SharedPreferencesKeys;
import com.avodev.awspushmanager.example.util.Utils;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver broadcastReceiver;

    private ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        // Add listeners
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save the new setting
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(SharedPreferencesKeys.PREFERENCE_NOTIFICATIONS, isChecked).apply();
            }
        });

        // Set push notifications
        setupPushNotifications();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Set togglebutton state
        toggleButton.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SharedPreferencesKeys.PREFERENCE_NOTIFICATIONS, true));
    }

    private void setupPushNotifications() {
        // Initialize Amazon Cognito
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                Configuration.COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                Regions.fromName(Configuration.AWS_REGION) // Region
        );

        // Initialize Amazon SNS
        AWSPushManager.setDefaultPlatformARN(Configuration.SNS_PLATFORM_ARN);
        AWSPushManager.initialize(this, new AmazonSNSAsyncClient(credentialsProvider));
        AWSPushManager.getAmazonSNSClient().setEndpoint(Configuration.AWS_ENDPOINT);
        AWSPushManager.registerTopicARNs(new String[] { Configuration.SNS_TOPIC_ARN });

        // Set up GCM
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v("GCM", "Broadcast received");

                if (intent.getAction().equals(RegistrationIntentService.BROADCAST_REGISTRATION_SUCCESSFUL)) {
                    Log.v("AWS", "Push registration successful");

                    // Register topics
                    for (final AWSPushTopic topic : AWSPushManager.getTopics()) {
                        topic.subscribe(new AWSPushTopic.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.v("AWS", "Topic subscription successful \n" + topic.getTopicName() + " \n" + topic.getTopicARN() + " \n" + topic.getSubscriptionARN());
                            }

                            @Override
                            public void onError(String error) {
                                Log.v("AWS", "Topic subscription failed " + error);
                            }
                        });
                    }
                } else if (intent.getAction().equals(RegistrationIntentService.BROADCAST_REGISTRATION_FAILED)) {
                    Log.v("AWS", "Push registration failed " + intent.getStringExtra("error"));
                }
            }
        };

        // Check if Google Play Services is available (optional)
        if (Utils.checkPlayServices(this)) {
            // Check if enabled (true by default)
            if (AWSPushManager.isEnabled()) {
                // Start IntentService to register this application with GCM.
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register for broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(RegistrationIntentService.BROADCAST_REGISTRATION_SUCCESSFUL));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(RegistrationIntentService.BROADCAST_REGISTRATION_FAILED));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }
}
