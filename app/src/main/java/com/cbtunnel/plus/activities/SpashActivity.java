package com.cbtunnel.plus.activities;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cbtunnel.plus.R;
import com.cbtunnel.plus.harliesApplication;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


@SuppressLint("NewApi")
public class SpashActivity extends AppCompatActivity {
    private static final String LOG_TAG = "SplashActivity";
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private final AtomicBoolean gatherConsentFinished = new AtomicBoolean(false);
    private AdsConsent googleMobileAdsConsentManager;
    private static final long COUNTER_TIME_MILLISECONDS = 5000;
    private long secondsRemaining;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        setContentView(R.layout.splash);

        createTimer();

        googleMobileAdsConsentManager = AdsConsent.getInstance(getApplicationContext());
        googleMobileAdsConsentManager.gatherConsent(this, consentError -> {
            if (consentError != null) {
                Log.w(LOG_TAG, String.format("%s: %s", consentError.getErrorCode(), consentError.getMessage()));
            }
            gatherConsentFinished.set(true);
            if (googleMobileAdsConsentManager.canRequestAds()) {
                initializeMobileAdsSdk();
            }

            if (secondsRemaining <= 0) {
                startMainActivity();
            }
        });

        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk();
        }
    }


    private void createTimer() {

        CountDownTimer countDownTimer = new CountDownTimer(COUNTER_TIME_MILLISECONDS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1;
            }

            @Override
            public void onFinish() {
                secondsRemaining = 0;
                Application application = getApplication();
                ((harliesApplication) application).showAdIfAvailable(SpashActivity.this, () -> {
                    startMainActivity();
                });
            }
        };
        countDownTimer.start();
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }

        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().build());
        new Thread(() -> {
            MobileAds.initialize(this, initializationStatus -> {});
            runOnUiThread(() -> {
                Application application = getApplication();
                ((harliesApplication) application).loadAd(this);
            });
        }).start();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, OpenVPNClient.class);
        this.startActivity(intent);
    }
}
