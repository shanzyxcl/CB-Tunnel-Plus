package com.cbtunnel.plus.activities;

import android.app.Activity;
import android.content.Context;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

public class AdsConsent {
    private static AdsConsent instance;
    private final ConsentInformation consentInformation;
    private AdsConsent(Context context) {
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }
    public static AdsConsent getInstance(Context context) {
        if (instance == null) {
            instance = new AdsConsent(context);
        }
        return instance;
    }
    public interface OnConsentGatheringCompleteListener {
        void consentGatheringComplete(FormError error);
    }
    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    public void gatherConsent(Activity activity, OnConsentGatheringCompleteListener onConsentGatheringCompleteListener) {
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
                .build();
        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build();
        consentInformation.requestConsentInfoUpdate(activity, params, () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, onConsentGatheringCompleteListener::consentGatheringComplete),
                onConsentGatheringCompleteListener::consentGatheringComplete);
    }
}
